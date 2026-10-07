/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.RecognitionSupportCallback
import android.speech.SpeechRecognizer
import timber.log.Timber

/**
 * Thin wrapper around [SpeechRecognizer].
 *
 * A fresh [SpeechRecognizer] instance is used for every session: both the system-side
 * RemoteSpeechRecognitionService and the service-side RecognitionService base class keep
 * per-listener session state that is not always cleared reliably once a session ended, which
 * makes reused instances hang silently on the next start. All framework exceptions are
 * contained: the session turns them into a user-visible error.
 */
class AndroidSpeechRecognizerEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /**
     * Warm up the selected RecognitionService without starting a session: the service is
     * bound and can prepare its engine. Uses the protocol's own checkRecognitionSupport.
     */
    fun checkRecognitionSupport(component: ComponentName?, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        var rec: SpeechRecognizer? = null
        try {
            rec = SpeechRecognizer.createSpeechRecognizer(context, component)
            val recognizer = rec
            // callbacks must be dispatched on the main thread; the recognizer is
            // released deterministically afterwards either way
            val released = java.util.concurrent.atomic.AtomicBoolean(false)
            val release = {
                if (released.compareAndSet(false, true)) {
                    recognizer.destroy()
                }
            }
            recognizer.checkRecognitionSupport(
                intent,
                android.os.Looper.getMainLooper().let { looper ->
                    java.util.concurrent.Executor { runnable -> android.os.Handler(looper).post(runnable) }
                },
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: android.speech.RecognitionSupport) {
                        Timber.d("support check ok: %s", recognitionSupport)
                        release()
                    }

                    override fun onError(error: Int) {
                        Timber.w("support check error: %d", error)
                        release()
                    }
                }
            )
            // timeout: a service that never answers must not leak the recognizer
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(release, SUPPORT_CHECK_TIMEOUT_MS)
        } catch (e: Exception) {
            Timber.w(e, "recognition support check failed")
            rec?.destroy()
        }
    }

    /**
     * Build the recognition intent; partial results are required for live composing.
     * Segmentation policy is not part of the request: it belongs to the recognition service.
     */
    fun buildIntent(
        languageCode: String
    ): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            if (languageCode.isNotBlank()) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        }

    fun startListening(
        component: ComponentName?,
        intent: Intent,
        listener: RecognitionListener
    ) {
        destroy()
        val created = SpeechRecognizer.createSpeechRecognizer(context, component)
        recognizer = created
        created.setRecognitionListener(listener)
        created.startListening(intent)
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            Timber.e(e, "stopListening failed")
        }
    }

    fun cancel() {
        try {
            recognizer?.cancel()
        } catch (e: Exception) {
            Timber.e(e, "cancel failed")
        }
    }

    fun destroy() {
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Timber.e(e, "destroy failed")
        }
        recognizer = null
    }

    companion object {
        private const val SUPPORT_CHECK_TIMEOUT_MS = 5_000L

        /**
         * Map framework error codes to normalized error kinds. Unknown codes (including
         * API-level-specific additions) collapse into [SpeechRecognitionError.UNKNOWN].
         */
        fun mapError(error: Int): SpeechRecognitionError = when (error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognitionError.NETWORK_TIMEOUT
            SpeechRecognizer.ERROR_NETWORK -> SpeechRecognitionError.NETWORK
            SpeechRecognizer.ERROR_AUDIO -> SpeechRecognitionError.AUDIO
            SpeechRecognizer.ERROR_SERVER -> SpeechRecognitionError.SERVER
            SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionError.CLIENT
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionError.NO_SPEECH
            SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognitionError.NO_MATCH
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionError.RECOGNIZER_BUSY
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionError.INSUFFICIENT_PERMISSIONS
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> SpeechRecognitionError.LANGUAGE
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechRecognitionError.LANGUAGE
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> SpeechRecognitionError.RECOGNIZER_BUSY
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> SpeechRecognitionError.SERVER
            SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> SpeechRecognitionError.UNKNOWN
            else -> SpeechRecognitionError.UNKNOWN
        }

        fun resultsText(bundle: Bundle?): String? =
            bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
    }
}
