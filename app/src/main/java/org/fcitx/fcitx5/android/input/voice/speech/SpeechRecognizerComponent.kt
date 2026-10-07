/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionService
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.TextFormatFlag
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.utils.toast
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import timber.log.Timber

/**
 * Voice input through the system `SpeechRecognizer` protocol (PR #899).
 *
 * Lifecycle — one coordinator, main thread only:
 *
 * ```
 * IDLE --start--> STARTING --onReadyForSpeech--> LISTENING --stop--> AWAITING --+
 *  ^                                                |                          |
 *  +--- cancel (field switch / keyboard hidden / tap) <-+                      |
 *  <-- results / error (protocol terminators) ---------------------------------+
 * ```
 *
 * Invariants:
 * - non-IDLE ⇒ a fresh recognizer instance exists (the framework keys sessions by
 *   listener binder, so instances are never reused)
 * - IDLE ⇒ recognizer destroyed; teardown is issued synchronously on every path
 *   into IDLE, using shutdown-cancel semantics so a restart never races stale
 *   framework state
 * - the microphone is owned by the RecognitionService (recorded with this input
 *   method's attribution source); the input method never records itself
 * - service availability is never polled: failures are handled where they appear
 *   (binder death drops the direct binding and terminates a running session)
 *
 * Works with any RecognitionService provider: the system default (e.g. Google) as
 * well as third-party services selected in the voice settings. Binding a
 * RecognitionService never switches the current input method.
 *
 * Interaction:
 * - toolbar microphone button: tap to start, tap again to finish, tap once more
 *   to abort
 * - partial results compose underlined; confirmed phrases commit, and the final
 *   result commits on arrival
 */
class SpeechRecognizerComponent :
    UniqueComponent<SpeechRecognizerComponent>(), Dependent,
    InputBroadcastReceiver, ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val service: FcitxInputMethodService by manager.inputMethodService()

    private val prefs = AppPrefs.getInstance()


    private var languageCode = ""

    private val engine by lazy { AndroidSpeechRecognizerEngine(context) }

    /** Normalizes the service-reported dBFS level for the wave animation. */
    private val volumeNormalizer = SpeechVolumeNormalizer()

    /**
     * Direct binding to the selected third-party RecognitionService.
     *
     * The system binds services for all clients, but without BIND_INCLUDE_CAPABILITIES the
     * service process stays in a background state that aggressive OEM freezers (e.g. ColorOS
     * Hans) can freeze mid-session, which stalls the audio pipe. Binding it here with the
     * foreground input method's capabilities keeps the process alive.
     *
     * Lifecycle follows input focus and session activity:
     * - a focused editor warms the service (process + engine via checkRecognitionSupport)
     * - an active session holds the binding
     * - after a session ends the binding is kept for [RELEASE_GRACE_MS] so follow-up
     *   dictation starts instantly, then released so the process can be reclaimed
     */
    private var serviceBinding: ServiceConnection? = null
    private var pendingRelease: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun acquireServiceBinding() {
        val component = selectedComponent() ?: return
        cancelPendingRelease()
        if (serviceBinding != null) return
        val intent = Intent(RecognitionService.SERVICE_INTERFACE).setComponent(component)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {}

            /**
             * The service process died. The connection is a dead handle now: it no
             * longer carries BIND_INCLUDE_CAPABILITIES into a restarted service
             * process, and as long as it is kept, acquireServiceBinding() refuses to
             * re-bind. Keeping it left the restarted service without the input
             * method's foreground boost, and OEM audio policies (ColorOS serves
             * silent audio to such background recorders) made every following
             * session capture silence. Drop it so the next warm-up or session
             * establishes a fresh, fully-capable binding.
             */
            override fun onServiceDisconnected(name: ComponentName) {
                dropStaleBinding(this)
            }

            override fun onBindingDied(name: ComponentName) {
                dropStaleBinding(this)
            }

            /**
             * The service returned null from onBind(): the connection exists but
             * carries no binder, so it is as unusable as a dead one — drop it the
             * same way instead of blocking future re-binds.
             */
            override fun onNullBinding(name: ComponentName) {
                dropStaleBinding(this)
            }
        }
        val flags = Context.BIND_AUTO_CREATE or Context.BIND_INCLUDE_CAPABILITIES
        try {
            if (context.bindService(intent, connection, flags)) {
                serviceBinding = connection
            } else {
                Timber.w("bindService to %s failed", component)
            }
        } catch (e: Exception) {
            Timber.w(e, "bindService to %s failed", component)
        }
    }

    /**
     * Called from the death callbacks of the direct binding
     * ([ServiceConnection.onServiceDisconnected]/[ServiceConnection.onBindingDied]/
     * [ServiceConnection.onNullBinding], main thread, like all binding state):
     * release the dead connection immediately instead of keeping it for the release
     * grace period, and terminate any running session — its state lives in the dead
     * process, so no callback can ever arrive. The framework never surfaces service
     * death to the RecognitionListener, so this is the moment to respond to the
     * failure; availability is not polled anywhere.
     */
    private fun dropStaleBinding(connection: ServiceConnection) {
        if (serviceBinding !== connection) return
        Timber.w("recognition service connection died; dropping stale binding")
        try {
            context.unbindService(connection)
        } catch (e: Exception) {
            Timber.w(e, "unbindService failed")
        }
        serviceBinding = null
        if (session.state != SpeechSessionState.IDLE) {
            session.onError(SpeechRecognitionError.CLIENT)
        }
    }

    private fun warmUpService() {
        val component = selectedComponent() ?: return
        Timber.d("warming up recognition service %s", component)
        acquireServiceBinding()
        engine.checkRecognitionSupport(component, engine.buildIntent(languageCode))
    }

    private fun scheduleServiceRelease() {
        cancelPendingRelease()
        pendingRelease = Runnable { releaseServiceBinding() }
            .also { mainHandler.postDelayed(it, RELEASE_GRACE_MS) }
    }

    private fun cancelPendingRelease() {
        pendingRelease?.let { mainHandler.removeCallbacks(it) }
        pendingRelease = null
    }

    private fun releaseServiceBinding() {
        cancelPendingRelease()
        serviceBinding?.let {
            try {
                context.unbindService(it)
            } catch (e: Exception) {
                Timber.w(e, "unbindService failed")
            }
        }
        serviceBinding = null
    }

    /** Notified with the normalized audio level while a session is listening. */
    fun interface AudioVolumeListener {
        fun onAudioVolumeChange(active: Boolean, level: Float)
    }

    private val audioVolumeListeners = WeakHashSet<AudioVolumeListener>()

    fun addAudioVolumeListener(listener: AudioVolumeListener) {
        audioVolumeListeners.add(listener)
    }

    private fun notifyAudioLevel(active: Boolean, level: Float) {
        mainHandler.post {
            audioVolumeListeners.forEach { it.onAudioVolumeChange(active, level) }
        }
    }

    private fun startEngine() {
        engine.startListening(
            selectedComponent(),
            engine.buildIntent(languageCode),
            recognitionListener
        )
    }

    /** Hides the wave animation; the microphone itself is owned by the service. */
    private fun stopAudioLevel() {
        notifyAudioLevel(false, 0f)
    }

    /** Notified while a session is running (STARTING..AWAITING_RESULTS). */
    fun interface VoiceInputStateListener {
        fun onVoiceInputActive(active: Boolean)
    }

    private val stateListeners = WeakHashSet<VoiceInputStateListener>()

    fun addVoiceInputStateListener(listener: VoiceInputStateListener) {
        stateListeners.add(listener)
    }

    fun toggle() {
        if (!ensureMicrophonePermission()) return
        session.toggle()
    }

    /**
     * Sessions need the microphone before anything else; with caller-provided audio the
     * service-side permission preflight is skipped, so the check has to happen here.
     * Failing it routes the user to the permission flow without starting a session.
     */
    private fun ensureMicrophonePermission(): Boolean {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestMicrophonePermission()
        }
        return granted
    }

    private fun requestMicrophonePermission() {
        // an input method cannot host the system permission dialog itself; the
        // transparent activity finishes as soon as the user answers
        context.startActivity(
            Intent(context, VoicePermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun isRecognitionServiceAvailable(): Boolean {
        val selected = selectedComponent()
        return if (selected != null) {
            RecognitionServiceRegistry.isAvailable(context, selected)
        } else {
            SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    private val preferredVoiceInput by prefs.keyboard.preferredVoiceInput
    private val showVoiceInputButton by prefs.keyboard.showVoiceInputButton

    init {
        // Switching the recognition service must not leave the keep-alive binding
        // pointing at the OLD service: it would hold that process alive for the
        // release grace while sessions bind the new one.
        prefs.keyboard.preferredVoiceInput.registerOnChangeListener { _, _ ->
            releaseServiceBinding()
        }
    }

    override fun onScopeSetupFinished(scope: org.mechdancer.dependency.DynamicScope) {
        // The keyboard accepting the preview (space etc.) advances the session's
        // committed prefix so the next partial does not re-commit the same text.
        // Registered here, NOT in the constructor: the service dependency only
        // resolves once this component is registered in the scope — touching it
        // earlier crashes input view creation and the keyboard never shows.
        service.voicePreviewAcceptedListener = { session.onPreviewAcceptedExternally() }
    }

    /**
     * The user-selected RecognitionService (the "preferred voice input" preference,
     * repurposed from voice-IME switching); null means the system default service.
     */
    private fun selectedComponent(): ComponentName? =
        RecognitionServiceRegistry.parseComponent(preferredVoiceInput)

    private companion object {
        const val STARTUP_TIMEOUT_MS = 3_000L
        const val FINALIZATION_TIMEOUT_MS = 5_000L
        const val RELEASE_GRACE_MS = 60_000L
    }

    private val delegate: SpeechRecognitionDelegate = object : SpeechRecognitionDelegate {
        override fun startRecognition() {
            previousPartialResult = ""
            volumeNormalizer.reset()
            acquireServiceBinding()
            startEngine()
            Timber.i(
                "startRecognition: service=%s",
                selectedComponent() ?: "system default"
            )
        }

        override fun stopRecognition() {
            // the service owns the microphone and finalizes on stopListening
            stopAudioLevel()
            engine.stopListening()
        }

        override fun cancelRecognition() {
            stopAudioLevel()
            // sessions are one-shot: destroy (shutdown-cancel) instead of plain cancel so
            // the system-side client entry is removed immediately instead of lingering
            engine.destroy()
        }

        override fun destroyRecognition() {
            stopAudioLevel()
            engine.destroy()
        }

        override fun setComposingText(text: String) {
            Timber.d("setComposingText: '%s'", text)
            service.updateComposingText(
                FormattedText(arrayOf(text), intArrayOf(TextFormatFlag.Underline.flag), -1),
                voicePreview = true
            )
        }

        override fun confirmComposingText() {
            Timber.d("confirmComposingText")
            // the preview is exactly the confirmed text: convert the composing
            // region in place. Deleting the preview and re-inserting the confirmed
            // part (the old approach) flickered the editor and desynchronized the
            // selection tracker on long dictations with many phrase boundaries.
            service.finishComposing()
        }

        override fun discardComposingText() {
            service.updateComposingText(FormattedText.Empty, voicePreview = true)
        }

        override fun commitText(text: String) {
            service.commitText(text)
        }

        override fun requestMicrophonePermission() {
            this@SpeechRecognizerComponent.requestMicrophonePermission()
        }

        override fun onAudioLevel(active: Boolean, level: Float) {
            notifyAudioLevel(active, level)
        }

        override fun notifyError(error: SpeechRecognitionError) {
            val res = when (error) {
                SpeechRecognitionError.NETWORK_TIMEOUT, SpeechRecognitionError.NETWORK ->
                    R.string.speech_error_network
                SpeechRecognitionError.AUDIO -> R.string.speech_error_audio
                SpeechRecognitionError.SERVER -> R.string.speech_error_server
                SpeechRecognitionError.NO_SPEECH -> R.string.speech_error_no_speech
                SpeechRecognitionError.NO_MATCH -> R.string.speech_error_no_match
                SpeechRecognitionError.RECOGNIZER_BUSY -> R.string.speech_error_busy
                SpeechRecognitionError.INSUFFICIENT_PERMISSIONS ->
                    R.string.voice_state_permission_required
                SpeechRecognitionError.LANGUAGE -> R.string.speech_error_language
                SpeechRecognitionError.CLIENT -> R.string.speech_error_unresponsive
                SpeechRecognitionError.UNKNOWN -> R.string.speech_error_generic
            }
            context.toast(res)
        }

        override fun onStateChanged(state: SpeechSessionState) {
            Timber.d("session state: %s", state)
            val active = state != SpeechSessionState.IDLE
            stateListeners.forEach { it.onVoiceInputActive(active) }
            // freeze detectors: binder death is already handled reactively when the
            // connection drops, so these only cover a service process that is FROZEN
            // while its binder stays alive (OEM freezers can do this even to bound
            // services) — no callbacks at all. STARTING must reach readyForSpeech
            // within 3 seconds; AWAITING_RESULTS must deliver results within 5
            // (finalization is at most one queued transcription, well under a second
            // even on slow devices).
            mainHandler.removeCallbacks(startupTimeout)
            mainHandler.removeCallbacks(finalizationTimeout)
            if (state == SpeechSessionState.STARTING) {
                mainHandler.postDelayed(startupTimeout, STARTUP_TIMEOUT_MS)
            } else if (state == SpeechSessionState.AWAITING_RESULTS) {
                mainHandler.postDelayed(finalizationTimeout, FINALIZATION_TIMEOUT_MS)
            }
            if (state == SpeechSessionState.IDLE) {
                // Sessions are one-shot: destroy the recognizer so the system unbinds
                // the RecognitionService; the next session then starts from clean state
                // on both sides.
                stopAudioLevel()
                scheduleServiceRelease()
                engine.destroy()
            }
        }
    }

    /**
     * Terminates a session that never left STARTING: the RecognitionService is
     * unresponsive (frozen by the OEM, killed, or crashed without a binder callback).
     */
    private val startupTimeout = Runnable {
        if (session.state == SpeechSessionState.STARTING) {
            Timber.w("speech service unresponsive after ${STARTUP_TIMEOUT_MS}ms; aborting")
            session.onError(SpeechRecognitionError.CLIENT)
        }
    }

    /**
     * Terminates a session that never received its final result: the service
     * acknowledged the stop (or signalled endOfSpeech) but then stopped calling
     * back — frozen, or crashed without the binder dying.
     */
    private val finalizationTimeout = Runnable {
        if (session.state == SpeechSessionState.AWAITING_RESULTS) {
            Timber.w("speech service never finalized after ${FINALIZATION_TIMEOUT_MS}ms; aborting")
            session.onError(SpeechRecognitionError.CLIENT)
        }
    }

    /** The previous partialResult text, used to detect phrase boundaries. */
    private var previousPartialResult = ""

    private val session = SpeechRecognitionSession(delegate)

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle) {
            session.onReadyForSpeech()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            // The service measures the audio level; conventions vary (our service sends
            // dBFS, Google sends roughly [-2,+10]) so the normalizer handles both.
            session.onRmsChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            session.onEndOfSpeech()
        }

        override fun onError(error: Int) {
            Timber.w("onError: %d", error)
            session.onError(AndroidSpeechRecognizerEngine.mapError(error))
        }

        override fun onPartialResults(partialResults: Bundle) {
            val text = AndroidSpeechRecognizerEngine.resultsText(partialResults) ?: return
            if (text.isBlank()) return
            // phrase boundary: new partial extends the previous one → the previous
            // non-blank suffix is confirmed; commit it and compose only the new part
            session.onPartialResultWithHistory(text, previousPartialResult)
            previousPartialResult = text
        }

        override fun onResults(results: Bundle) {
            Timber.i("onResults")
            session.onResult(AndroidSpeechRecognizerEngine.resultsText(results))
        }

        // segmented-session callbacks are interface-required but never used:
        // this client never requests EXTRA_SEGMENTED_SESSION, so a well-behaved
        // service does not deliver them
        override fun onSegmentResults(segmentResults: Bundle) {}

        override fun onEndOfSegmentedSession() {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        languageCode = ime.languageCode.replace('_', '-')
    }

    /** Password fields never offer voice input; recorded for the warm-up decision. */
    private var currentFieldIsPassword = false

    override fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {
        // never let results of one input field leak into the next one
        session.cancel()
        currentFieldIsPassword = capFlags.has(CapabilityFlag.Password)
    }

    override fun onWindowAttached(window: InputWindow) {
        // The keyboard is visible and the mic button is reachable — the precise
        // moment voice input becomes possible. Warm the service (process + engine)
        // here rather than on editor focus: focus fires for password fields and
        // for editors served without showing the keyboard (e.g. hardware keys),
        // which would load the model for nothing. Warm-up is idempotent.
        if (!currentFieldIsPassword && showVoiceInputButton) {
            warmUpService()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        // the keyboard is going away and the user loses the stop affordance: the
        // session and the microphone must not outlive the view
        session.destroy()
    }
}
