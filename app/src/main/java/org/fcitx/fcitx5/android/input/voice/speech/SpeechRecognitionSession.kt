/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import timber.log.Timber

/**
 * Error kinds normalized from `android.speech.SpeechRecognizer` error codes, so the session
 * state machine can be tested on the JVM without the Android framework.
 */
enum class SpeechRecognitionError {
    NETWORK_TIMEOUT,
    NETWORK,
    AUDIO,
    SERVER,
    CLIENT,
    NO_SPEECH,
    NO_MATCH,
    RECOGNIZER_BUSY,
    INSUFFICIENT_PERMISSIONS,
    LANGUAGE,
    UNKNOWN
}

/** Lifecycle of one speech recognition session. */
enum class SpeechSessionState {
    /** No session; a new one can be started. */
    IDLE,

    /** [SpeechRecognitionSession.start] was called; waiting for `onReadyForSpeech`. */
    STARTING,

    /** Recognition is running; partial results and audio levels are expected. */
    LISTENING,

    /** Speech ended or the user asked to finish; waiting for final `onResults`/`onError`. */
    AWAITING_RESULTS
}

/**
 * Android-facing operations the session drives. All text output goes through this interface,
 * which keeps the state machine pure and unit-testable.
 */
interface SpeechRecognitionDelegate {
    /**
     * Create and start the engine for the currently selected RecognitionService.
     * May throw [RuntimeException] if the engine cannot be created or started.
     */
    fun startRecognition()

    /** Ask the engine to finalize and deliver results. */
    fun stopRecognition()

    /** Ask the engine to abort without delivering results. */
    fun cancelRecognition()

    /** Destroy the engine so the next session starts from a fresh instance. */
    fun destroyRecognition()

    /** Show [text] as composing (underlined) text, replacing previous partials. */
    fun setComposingText(text: String)

    /**
     * Make the current composing preview permanent in place. The session only ever
     * confirms the WHOLE preview (the previous partial in full), so converting the
     * composing region where it stands is exact — no delete-and-reinsert round
     * trip, which flickered and desynchronized the selection tracker over long
     * dictations with many phrase boundaries.
     */
    fun confirmComposingText()

    /** Remove any partial composing text without committing it. */
    fun discardComposingText()

    /** Commit the final result into the editor. */
    fun commitText(text: String)

    /** RECORD_AUDIO is missing; guide the user to the permission flow. */
    fun requestMicrophonePermission()

    /** Surface a user-visible error message. */
    fun onAudioLevel(active: Boolean, level: Float)

    fun notifyError(error: SpeechRecognitionError)

    /** Session state transition; [SpeechSessionState.IDLE] means the session is over. */
    fun onStateChanged(state: SpeechSessionState)

}


/**
 * Speech recognition session state machine.
 *
 * Cross-implementation contract enforced here (see RecognitionService research):
 * - A session only terminates via [onResult] or [onError]; `onEndOfSpeech` is a
 *   pure notification (implementations disagree wildly: Google follows it with
 *   results, others fire it per phrase while listening on).
 * - Callbacks that arrive after the session has ended (late races after `cancel`) are ignored,
 *   so stale results are never committed into a new input field.
 * - `INSUFFICIENT_PERMISSIONS` is routed to permission handling; engine-destroying errors
 *   (`CLIENT`, `RECOGNIZER_BUSY`) force a fresh engine for the next session.
 */
class SpeechRecognitionSession(
    private val delegate: SpeechRecognitionDelegate,
    private val volumeNormalizer: SpeechVolumeNormalizer = SpeechVolumeNormalizer()
) {
    /**
     * The portion of the transcript that has already been committed as permanent text.
     * Grows as the recognizer confirms phrases: each new partialResult that extends the
     * previous one confirms the old suffix, which gets committed; only the newest
     * phrase stays as composing (underlined).
     */
    private var committedPrefix = ""
    var state = SpeechSessionState.IDLE
        private set

    /** Start a session from the idle state. No-op otherwise. */
    fun start() {
        if (state != SpeechSessionState.IDLE) return
        committedPrefix = ""
        setState(SpeechSessionState.STARTING)
        volumeNormalizer.reset()
        try {
            delegate.startRecognition()
        } catch (e: RuntimeException) {
            Timber.e(e, "startRecognition failed")
            delegate.destroyRecognition()
            setState(SpeechSessionState.IDLE)
            delegate.notifyError(SpeechRecognitionError.CLIENT)
        }
    }

    /** Toggle semantics for a tap on the toolbar microphone button. */
    fun toggle() {
        when (state) {
            SpeechSessionState.IDLE -> start()
            // user changed their mind before the engine became ready
            SpeechSessionState.STARTING -> cancel()
            // stop listening but keep the session alive until results arrive
            SpeechSessionState.LISTENING -> stop()
            // user does not want the pending result; abort it
            SpeechSessionState.AWAITING_RESULTS -> cancel()
        }
    }

    /**
     * Finish the active session and wait for the final result.
     *
     * Also valid in [SpeechSessionState.AWAITING_RESULTS]: repeated calls only
     * re-assert the stop; the service owns the microphone and its stopListening
     * is idempotent.
     */
    fun stop() {
        when (state) {
            SpeechSessionState.LISTENING, SpeechSessionState.STARTING -> {
                setState(SpeechSessionState.AWAITING_RESULTS)
                delegate.stopRecognition()
            }
            SpeechSessionState.AWAITING_RESULTS -> {
                // the service signalled end of speech before the user stopped
                delegate.stopRecognition()
            }
            SpeechSessionState.IDLE -> {}
        }
    }

    /** Abort the session and drop any partial result. */
    fun cancel() {
        if (state == SpeechSessionState.IDLE) return
        delegate.cancelRecognition()
        finish(dropComposing = true)
    }

    /** Tear everything down; called when the input view goes away. */
    fun destroy() {
        if (state != SpeechSessionState.IDLE) {
            delegate.cancelRecognition()
        }
        delegate.destroyRecognition()
        finish(dropComposing = true)
    }

    private fun finish(dropComposing: Boolean) {
        if (dropComposing) delegate.discardComposingText()
        setState(SpeechSessionState.IDLE)
    }

    fun onReadyForSpeech() {
        if (state == SpeechSessionState.STARTING) {
            setState(SpeechSessionState.LISTENING)
        }
    }

    /**
     * The composing preview currently shown (the part of the last partial beyond
     * [committedPrefix]); tracked so an external acceptance can advance the prefix.
     */
    private var previewText = ""

    /**
     * Handles a partialResult with the previous partial for reference.
     * When the new text extends the previous one (a phrase was confirmed and a new
     * phrase started), the confirmed portion is committed as permanent text and only
     * the newest phrase shows as composing (underlined).
     */
    fun onPartialResultWithHistory(text: String, previousText: String) {
        if (state == SpeechSessionState.IDLE) return
        if (text.isBlank()) return

        if (text.startsWith(previousText) && previousText.isNotEmpty()) {
            // the new text extends the previous one: the previous preview (in full)
            // is confirmed — the delegate converts the composing region in place.
            // An empty preview (externally accepted) needs no conversion.
            if (previewText.isNotEmpty()) {
                delegate.confirmComposingText()
                previewText = ""
            }
            committedPrefix = previousText
            // only the new part composes
            val newPart = text.substring(committedPrefix.length)
            if (newPart.isNotBlank()) {
                previewText = newPart
                delegate.setComposingText(newPart)
            }
        } else if (text.startsWith(committedPrefix)) {
            // simple update: show the new suffix as composing
            val newPart = text.substring(committedPrefix.length)
            if (newPart.isNotBlank()) {
                previewText = newPart
                delegate.setComposingText(newPart)
            }
        } else {
            // transcript was revised: show everything as composing again
            committedPrefix = ""
            previewText = text
            delegate.setComposingText(text)
        }
    }

    /**
     * The keyboard accepted the composing preview externally (e.g. the user pressed
     * space to take the latest phrase): the preview text is now permanent in the
     * editor. [committedPrefix] must advance with it, or the next partial would
     * re-commit the same text as a "confirmed tail" — duplicating everything.
     */
    fun onPreviewAcceptedExternally() {
        if (state == SpeechSessionState.IDLE) return
        if (previewText.isEmpty()) return
        committedPrefix += previewText
        previewText = ""
    }

    fun onPartialResult(text: String?) {
        if (state == SpeechSessionState.IDLE) return
        if (text.isNullOrBlank()) return
        onPartialResultWithHistory(text, committedPrefix)
    }

    fun onEndOfSpeech() {
        // Pure notification, NOT "results are imminent": Google delivers results
        // right after it, but other implementations (e.g. BiBi) fire it at every
        // phrase boundary while the session keeps listening until the user stops.
        // Treating it as a terminator-or-precursor froze the wave and armed the
        // finalization timeout on services that legitimately keep running,
        // killing dictations mid-speech. The session stays LISTENING; results or
        // the user's stop end it.
    }

    /**
     * A finalized segment arrived while the session keeps running (segmented session mode):
     * commit it immediately; recognition continues.
     */
    fun onRmsChanged(db: Float) {
        if (state != SpeechSessionState.LISTENING && state != SpeechSessionState.STARTING) return
        delegate.onAudioLevel(true, volumeNormalizer.normalize(db))
    }

    fun onResult(text: String?) {
        if (state == SpeechSessionState.IDLE) return
        when {
            text.isNullOrBlank() -> {
                // no match: drop the partial text instead of committing it
                delegate.discardComposingText()
            }
            else -> {
                // the confirmed prefix was already committed incrementally as
                // phrases were confirmed; commit only the remainder, or the
                // final result would duplicate everything on top of it.
                // (A revision that does not extend the committed prefix reset
                // it in onPartialResultWithHistory, leaving the full text.)
                val remainder =
                    if (text.startsWith(committedPrefix)) text.substring(committedPrefix.length) else text
                if (remainder.isBlank()) {
                    // the final result is exactly what was already committed
                    delegate.discardComposingText()
                } else {
                    delegate.commitText(remainder)
                }
            }
        }
        finish(dropComposing = false)
    }

    fun onError(error: SpeechRecognitionError) {
        if (state == SpeechSessionState.IDLE) return
        when (error) {
            SpeechRecognitionError.INSUFFICIENT_PERMISSIONS ->
                delegate.requestMicrophonePermission()
            // engine is in an unknown state; force recreation for the next session
            SpeechRecognitionError.CLIENT, SpeechRecognitionError.RECOGNIZER_BUSY ->
                delegate.destroyRecognition()
            else -> {}
        }
        finish(dropComposing = true)
        delegate.notifyError(error)
    }

    private fun setState(state: SpeechSessionState) {
        this.state = state
        delegate.onStateChanged(state)
    }

}
