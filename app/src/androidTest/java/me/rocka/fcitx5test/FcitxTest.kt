package me.rocka.fcitx5test

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class FcitxTest {


    private companion object {

        lateinit var fcitx: Fcitx
        val lifeCycleOwner = TestLifecycleOwner()
        val fcitxEventChannel = Channel<FcitxEvent<*>>(capacity = Channel.CONFLATED)

        fun log(str: String) = Log.d("UnitTest", str)

        @BeforeClass
        @JvmStatic
        fun setup() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            fcitx = Fcitx(context)
            lifeCycleOwner.lifecycle.addObserver(fcitx)
            lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            // forward to our channel for point to point consuming
            fcitx.eventFlow
                .onEach { fcitxEventChannel.send(it) }
                .launchIn(GlobalScope)

            // wait fcitx started
            runBlocking { delay(2000) }
        }

        @AfterClass
        @JvmStatic
        fun cleanup() {
            log("cleanup")
            lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        private suspend fun sendString(str: String) {
            str.forEach { c ->
                fcitx.sendKey(c)
                delay(200)
            }
        }

        private suspend inline fun <reified T : FcitxEvent<T>> receiveFirst(): T? =
            fcitxEventChannel.receiveAsFlow().mapNotNull { it as? T }.firstOrNull()

        private suspend fun receiveFirstCandidateList() =
            receiveFirst<FcitxEvent.CandidateListEvent>()

        private suspend fun receiveFirstCommitString() =
            receiveFirst<FcitxEvent.CommitStringEvent>()

        private suspend fun receiveFirstPreedit() = receiveFirst<FcitxEvent.PreeditEvent>()

        private suspend fun receiveFirstInputPanelAux() =
            receiveFirst<FcitxEvent.InputPanelAuxEvent>()

    }


    @Test
    fun testPinyin(): Unit = runBlocking {
        sendString("nihaoshijie")
        val expected = "你好世界"
        val candidateList = receiveFirstCandidateList()?.data
        log("candidateList is $candidateList")
        Assert.assertEquals(expected, candidateList?.firstOrNull())
        fcitx.select(0)
        val commitString = receiveFirstCommitString()?.data
        log("commitString is $commitString")
        Assert.assertEquals(expected, commitString)
        fcitx.reset()
    }

    @Test
    fun testInputPanelStatus(): Unit = runBlocking {
        fcitx.reset()
        log("after first reset: ${fcitx.isEmpty()}")
        Assert.assertEquals(true, fcitx.isEmpty())
        fcitx.sendKey('a')
        receiveFirstCandidateList()
        log("after sending 'a': ${fcitx.isEmpty()}")
        Assert.assertEquals(false, fcitx.isEmpty())
        fcitx.reset()
        receiveFirstCandidateList()
        log("after second reset: ${fcitx.isEmpty()}")
        Assert.assertEquals(true, fcitx.isEmpty())
    }


}