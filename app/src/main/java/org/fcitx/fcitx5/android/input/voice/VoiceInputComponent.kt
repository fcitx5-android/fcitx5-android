/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.voice

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.InputMethodSubtype
import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.TextFormatFlag
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.toast
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import timber.log.Timber

class VoiceInputComponent : UniqueComponent<VoiceInputComponent>(), Dependent,
    ManagedHandler by managedHandler(), InputBroadcastReceiver {

    val context by manager.context()
    val service by manager.inputMethodService()
    val fcitx by manager.fcitx()

    private val prefs = AppPrefs.getInstance()

    private val showVoiceInputButton by prefs.keyboard.showVoiceInputButton
    private val preferredVoiceInput by prefs.keyboard.preferredVoiceInput

    private var voiceInputSubtype: Pair<String, InputMethodSubtype>? = null

    fun shouldShowVoiceInput(capFlags: CapabilityFlags): Boolean {
        voiceInputSubtype = InputMethodUtil.findVoiceSubtype(preferredVoiceInput)
        return showVoiceInputButton && voiceInputSubtype != null && !capFlags.has(CapabilityFlag.Password)
    }

    // TODO: switch between "other voice input method" and "SpeechRecognizer"
    val voiceInputCallback = View.OnClickListener {
        val preferredIdx = InputMethodUtil.listVoiceInputMethods().indexOfFirst { (imi, subType) ->
            imi.id == preferredVoiceInput
        }
        if (preferredIdx < 0) {
            startListening()
            return@OnClickListener
        }
        val (id, subtype) = voiceInputSubtype ?: return@OnClickListener
        InputMethodUtil.switchInputMethod(service, id, subtype)
    }

    private var languageCode = ""

    override fun onImeUpdate(ime: InputMethodEntry) {
        languageCode = ime.languageCode.replace("_", "-")
    }

    fun Bundle.results() = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

    fun Bundle.dumpResults() = buildString {
        val results = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val scores = getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        results?.forEachIndexed { index, string ->
            append(string)
            append("[score=${scores?.get(index)}]")
            if (index > 0) append(", ")
        }
    }

    // TODO: destroy recognizer onFinishInput
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }
    }

    private var startedListening = false

    private fun buildUnderlineText(str: String): FormattedText {
        return FormattedText(arrayOf(str), intArrayOf(TextFormatFlag.Underline.flag), -1)
    }

    fun interface AudioVolumeListener {
        fun onAudioVolumeChange(listening: Boolean, dB: Float)
    }

    private val audioVolumeListeners = WeakHashSet<AudioVolumeListener>()

    fun addAudioVolumeListener(listener: AudioVolumeListener) {
        audioVolumeListeners.add(listener)
    }

    fun removeAudioVolumeListener(listener: AudioVolumeListener) {
        audioVolumeListeners.remove(listener)
    }

    // TODO: interrupt voice input on keyboard input
    private val recognitionListener by lazy {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle) {
                startedListening = true
                audioVolumeListeners.forEach { it.onAudioVolumeChange(true, 0f) }
                Timber.d("onReadyForSpeech, $params")
            }

            override fun onBeginningOfSpeech() {
                Timber.d("onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Timber.d("onRmsChanged: rmsdB=$rmsdB")
                audioVolumeListeners.forEach { it.onAudioVolumeChange(true, rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray) {
                /* This would never be called */
            }

            override fun onEndOfSpeech() {
                Timber.d("onEndOfSpeech")
            }

            override fun onError(error: Int) {
                startedListening = false
                audioVolumeListeners.forEach { it.onAudioVolumeChange(false, 0f) }
                Timber.d("onError: $error")
                context.toast("onError: $error")
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        // TODO: launch activity to request permission
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                    }
                    else -> {
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                Timber.d("onPartialResults: ${partialResults.dumpResults()}")
                val strings = partialResults.results() ?: return
                // TODO: don't call IMS directly
                service.updateComposingText(buildUnderlineText(strings[0]))
            }

            override fun onResults(results: Bundle) {
                startedListening = false
                audioVolumeListeners.forEach { it.onAudioVolumeChange(false, 0f) }
                Timber.d("onResults: ${results.dumpResults()}")
                val strings = results.results() ?: return
                service.commitText(strings[0])
            }

            override fun onEvent(eventType: Int, params: Bundle) {
                /* unused */
            }
        }
    }

    fun startListening() {
        if (startedListening) {
            startedListening = false
            speechRecognizer.stopListening()
            return
        }

        speechRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // required
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // optional
            if (languageCode.isNotBlank()) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        })
    }
}
