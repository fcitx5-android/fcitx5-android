/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecognitionSessionTest {

    private sealed interface Op {
        data object Start : Op
        data object Stop : Op
        data object Cancel : Op
        data object Destroy : Op
        data class Compose(val text: String) : Op
        data object Discard : Op
        data object Confirm : Op
        data class Commit(val text: String) : Op
        data object RequestPermission : Op
        data class Notify(val error: SpeechRecognitionError) : Op
    }

    private class FakeDelegate(var startThrows: RuntimeException? = null) : SpeechRecognitionDelegate {
        val ops = mutableListOf<Op>()
        val states = mutableListOf<SpeechSessionState>()
        override fun startRecognition() {
            ops += Op.Start
            startThrows?.let { throw it }
        }
        override fun stopRecognition() {
            ops += Op.Stop
        }
        override fun cancelRecognition() {
            ops += Op.Cancel
        }
        override fun destroyRecognition() {
            ops += Op.Destroy
        }
        override fun setComposingText(text: String) {
            ops += Op.Compose(text)
        }
        override fun discardComposingText() {
            ops += Op.Discard
        }
        override fun commitText(text: String) {
            ops += Op.Commit(text)
        }
        override fun requestMicrophonePermission() {
            ops += Op.RequestPermission
        }
        override fun notifyError(error: SpeechRecognitionError) {
            ops += Op.Notify(error)
        }
        override fun onAudioLevel(active: Boolean, level: Float) {
        }
        override fun confirmComposingText() {
            ops += Op.Confirm
        }
        override fun onStateChanged(state: SpeechSessionState) {
            states += state
        }
    }

    private fun listeningSession(delegate: FakeDelegate): SpeechRecognitionSession {
        val session = SpeechRecognitionSession(delegate)
        session.start()
        session.onReadyForSpeech()
        return session
    }

    @Test
    fun `start goes through STARTING to LISTENING`() {
        val delegate = FakeDelegate()
        val session = SpeechRecognitionSession(delegate)
        session.start()
        assertEquals(SpeechSessionState.STARTING, session.state)
        session.onReadyForSpeech()
        assertEquals(SpeechSessionState.LISTENING, session.state)
        assertEquals(listOf(Op.Start), delegate.ops.filterIsInstance<Op.Start>())
    }

    @Test
    fun `partial results become composing text and final result is committed`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResult("ni h")
        session.onPartialResult("ni hao")
        session.onResult("ni hao shi jie")
        assertEquals(
            listOf(Op.Compose("ni h"), Op.Compose("ni hao"), Op.Commit("ni hao shi jie")),
            delegate.ops.filterIsInstance<Op.Compose>() + delegate.ops.filterIsInstance<Op.Commit>()
        )
        assertEquals(SpeechSessionState.IDLE, session.state)
    }

    @Test
    fun `toggle while listening stops the engine and waits for results`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.toggle()
        assertEquals(SpeechSessionState.AWAITING_RESULTS, session.state)
        assertTrue(delegate.ops.contains(Op.Stop))
        assertFalse(delegate.ops.contains(Op.Cancel))
        session.onResult("hi")
        assertTrue(delegate.ops.contains(Op.Commit("hi")))
    }

    @Test
    fun `toggle while awaiting results aborts and drops composing text`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResult("abc")
        session.toggle() // stop
        session.toggle() // abort
        assertEquals(SpeechSessionState.IDLE, session.state)
        assertTrue(delegate.ops.contains(Op.Cancel))
        assertTrue(delegate.ops.contains(Op.Discard))
        assertFalse(delegate.ops.contains(Op.Commit("abc")))
    }

    @Test
    fun `onEndOfSpeech is notification only and never stops the engine`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onEndOfSpeech()
        // implementations disagree: some follow it with results, others (e.g. BiBi)
        // fire it at every phrase while the session keeps listening until the user
        // stops — so it must NOT move the session, freeze the wave, or arm any
        // finalization timeout
        assertEquals(SpeechSessionState.LISTENING, session.state)
        assertFalse(delegate.ops.contains(Op.Stop))
        assertFalse(delegate.ops.contains(Op.Cancel))
        // partials and audio level keep flowing after the notification
        session.onPartialResultWithHistory("你好", "")
        assertTrue(delegate.ops.contains(Op.Compose("你好")))
        session.onRmsChanged(-30f)
        // the user's stop still finalizes
        session.stop()
        assertEquals(SpeechSessionState.AWAITING_RESULTS, session.state)
        assertTrue(delegate.ops.contains(Op.Stop))
    }

    @Test
    fun `blank result discards composing instead of committing`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResult("abc")
        session.onResult("  ")
        assertTrue(delegate.ops.contains(Op.Discard))
        assertFalse(delegate.ops.any { it is Op.Commit })
    }

    @Test
    fun `callbacks after the session ended are ignored`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onResult("done")
        session.onResult("stale")
        session.onPartialResult("stale")
        session.onError(SpeechRecognitionError.NETWORK)
        assertEquals(1, delegate.ops.count { it is Op.Commit })
        assertFalse(delegate.ops.any { it is Op.Compose })
        assertEquals(SpeechSessionState.IDLE, session.state)
    }

    @Test
    fun `cancel drops composing text`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResult("partial")
        session.cancel()
        assertEquals(SpeechSessionState.IDLE, session.state)
        assertTrue(delegate.ops.contains(Op.Cancel))
        assertTrue(delegate.ops.contains(Op.Discard))
        assertFalse(delegate.ops.any { it is Op.Commit })
    }

    @Test
    fun `stop after endOfSpeech still finalizes`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onEndOfSpeech()
        // hold-to-talk release after the service signalled end of speech
        session.stop()
        assertTrue(delegate.ops.contains(Op.Stop))
        assertEquals(SpeechSessionState.AWAITING_RESULTS, session.state)
    }

    @Test
    fun `stop is idempotent while awaiting results`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.stop()
        session.stop()
        assertEquals(2, delegate.ops.count { it is Op.Stop })
        assertEquals(SpeechSessionState.AWAITING_RESULTS, session.state)
    }

    @Test
    fun `second phrase commits the first as permanent text`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        // first phrase arrives as partial
        session.onPartialResultWithHistory("今天天气", "")
        // verify: all of "今天天气" is composing
        assertEquals(listOf(Op.Compose("今天天气")),
            delegate.ops.filterIsInstance<Op.Compose>())

        // second phrase arrives: "今天天气 很好"
        session.onPartialResultWithHistory("今天天气 很好", "今天天气")
        // verify: the preview was confirmed in place, " 很好" is composing
        assertTrue(delegate.ops.contains(Op.Confirm))
        assertTrue(delegate.ops.contains(Op.Compose(" 很好")))
        // the confirm must happen BEFORE the new composing
        val confirmIdx = delegate.ops.indexOfFirst { it == Op.Confirm }
        val composeIdx = delegate.ops.indexOfLast { it == Op.Compose(" 很好") }
        assertTrue(confirmIdx < composeIdx)
    }

    @Test
    fun `third phrase commits the second`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("A", "")
        session.onPartialResultWithHistory("A B", "A")
        session.onPartialResultWithHistory("A B C", "A B")
        // both previews confirmed in place, " C" composing
        assertEquals(2, delegate.ops.count { it == Op.Confirm })
        assertTrue(delegate.ops.contains(Op.Compose(" C")))
    }

    @Test
    fun `revised transcript resets to full composing`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("ABC", "")
        session.onPartialResultWithHistory("ABD", "ABC") // "ABD" doesn't start with "ABC"
        // revision: everything shows as composing
        val lastCompose = delegate.ops.filterIsInstance<Op.Compose>().last()
        assertEquals("ABD", lastCompose.text)
        // no prefix commit happened for the revision
        assertFalse(delegate.ops.contains(Op.Commit("ABC")))
    }

    @Test
    fun `empty new part does not set composing`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("你好", "")
        session.onPartialResultWithHistory("你好", "你好") // same text
        // only one compose call (the second is a no-op since new part is blank)
        assertEquals(1, delegate.ops.count { it is Op.Compose })
    }

    /**
     * The confirmed prefix is committed incrementally while dictating with pauses;
     * the final result must commit ONLY the not-yet-committed remainder, otherwise
     * the release duplicates everything already on screen.
     */
    @Test
    fun `final result commits only the uncommitted remainder`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        // phrase 1 arrives, phrase 2 extends it: the prefix is committed incrementally
        session.onPartialResultWithHistory("你好", "")
        session.onPartialResultWithHistory("你好世界", "你好")
        assertTrue(delegate.ops.contains(Op.Confirm))

        session.onResult("你好世界今天")

        // the final result must NOT re-commit the already-committed prefix
        assertFalse(delegate.ops.contains(Op.Commit("你好世界今天")))
        // only the remainder after the committed prefix is committed
        assertTrue(delegate.ops.contains(Op.Commit("世界今天")))
    }

    @Test
    fun `final result shrinking to the committed prefix discards the preview`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("你好", "")
        session.onPartialResultWithHistory("你好世界", "你好")
        session.onPartialResultWithHistory("你好世界今天", "你好世界")

        // the final result is exactly what was already committed
        session.onResult("你好世界")

        assertTrue(delegate.ops.contains(Op.Discard))
        // the incremental confirms happened: no remainder, no full commit
        assertEquals(2, delegate.ops.count { it is Op.Confirm })
        assertTrue(delegate.ops.filterIsInstance<Op.Commit>().isEmpty())
    }

    @Test
    fun `single-phrase final still commits the full text`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("你好", "")

        session.onResult("你好呀")

        assertTrue(delegate.ops.contains(Op.Commit("你好呀")))
    }

    /**
     * The keyboard accepted the preview externally (space): the committed prefix
     * advances with it, so a following partial must NOT re-commit the preview as
     * a "confirmed tail" — that duplicated the whole phrase.
     */
    @Test
    fun `externally accepted preview is not re-committed by the next partial`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("第一次", "")
        session.onPreviewAcceptedExternally()

        // the next partial extends the transcript; the preview was already made
        // permanent by the acceptance, so only the new part may compose
        session.onPartialResultWithHistory("第一次 第二次", "第一次")

        assertEquals(0, delegate.ops.count { it is Op.Confirm })
        assertTrue(delegate.ops.filterIsInstance<Op.Compose>().any { it.text == " 第二次" })

        // and the final result still commits only the remainder
        session.onResult("第一次 第二次")
        assertTrue(delegate.ops.contains(Op.Commit(" 第二次")))
        val committed = delegate.ops.filterIsInstance<Op.Commit>().map { it.text }
        assertEquals(listOf(" 第二次"), committed)
    }

    @Test
    fun `external acceptance from idle is a no-op`() {
        val delegate = FakeDelegate()
        val session = SpeechRecognitionSession(delegate)
        session.onPreviewAcceptedExternally()
        assertTrue(delegate.ops.isEmpty())
    }

    @Test
    fun `destroy cancels an active session and destroys the engine`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.destroy()
        assertTrue(delegate.ops.contains(Op.Cancel))
        assertTrue(delegate.ops.contains(Op.Destroy))
        assertEquals(SpeechSessionState.IDLE, session.state)
    }

    @Test
    fun `destroy from idle is harmless`() {
        val delegate = FakeDelegate()
        val session = SpeechRecognitionSession(delegate)
        session.destroy()
        assertEquals(SpeechSessionState.IDLE, session.state)
        // only the engine release runs; no cancel was needed
        assertTrue(delegate.ops.contains(Op.Destroy))
        assertFalse(delegate.ops.contains(Op.Cancel))
    }

    /**
     * The service process dying mid-session surfaces as a CLIENT error (the binding
     * observes the death; the framework never notifies the listener): the session
     * must terminate, drop the composing preview, force a fresh engine for the next
     * session and surface the error — instead of hanging without callbacks.
     */
    @Test
    fun `service death during a session terminates it with a client error`() {
        val delegate = FakeDelegate()
        val session = listeningSession(delegate)
        session.onPartialResultWithHistory("你好", "")

        session.onError(SpeechRecognitionError.CLIENT)

        assertEquals(SpeechSessionState.IDLE, session.state)
        assertTrue(delegate.ops.contains(Op.Destroy))
        assertTrue(delegate.ops.contains(Op.Discard))
        assertTrue(delegate.ops.contains(Op.Notify(SpeechRecognitionError.CLIENT)))
        assertFalse(delegate.ops.contains(Op.Commit("你好")))
    }

    @Test
    fun `client error from idle is a no-op`() {
        val delegate = FakeDelegate()
        val session = SpeechRecognitionSession(delegate)
        session.onError(SpeechRecognitionError.CLIENT)
        assertEquals(SpeechSessionState.IDLE, session.state)
        assertFalse(delegate.ops.contains(Op.Notify(SpeechRecognitionError.CLIENT)))
    }
}
