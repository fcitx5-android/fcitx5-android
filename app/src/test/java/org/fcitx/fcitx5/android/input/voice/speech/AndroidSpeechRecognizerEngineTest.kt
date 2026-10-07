/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidSpeechRecognizerEngineTest {

    @Test
    fun `framework error codes map to normalized kinds`() {
        val cases = mapOf(
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT to SpeechRecognitionError.NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NETWORK to SpeechRecognitionError.NETWORK,
            SpeechRecognizer.ERROR_AUDIO to SpeechRecognitionError.AUDIO,
            SpeechRecognizer.ERROR_SERVER to SpeechRecognitionError.SERVER,
            SpeechRecognizer.ERROR_CLIENT to SpeechRecognitionError.CLIENT,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT to SpeechRecognitionError.NO_SPEECH,
            SpeechRecognizer.ERROR_NO_MATCH to SpeechRecognitionError.NO_MATCH,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY to SpeechRecognitionError.RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to SpeechRecognitionError.INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED to SpeechRecognitionError.LANGUAGE,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE to SpeechRecognitionError.LANGUAGE,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS to SpeechRecognitionError.RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED to SpeechRecognitionError.SERVER,
            SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS to SpeechRecognitionError.UNKNOWN
        )
        cases.forEach { (code, kind) ->
            assertEquals("code $code", kind, AndroidSpeechRecognizerEngine.mapError(code))
        }
    }

    @Test
    fun `unknown codes map to unknown`() {
        assertEquals(
            SpeechRecognitionError.UNKNOWN,
            AndroidSpeechRecognizerEngine.mapError(-1)
        )
        assertEquals(
            SpeechRecognitionError.UNKNOWN,
            AndroidSpeechRecognizerEngine.mapError(99)
        )
    }
}
