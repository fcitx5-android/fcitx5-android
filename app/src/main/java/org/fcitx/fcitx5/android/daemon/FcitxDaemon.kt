package org.fcitx.fcitx5.android.daemon

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.*
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object FcitxDaemon {

    private val realFcitx by lazy { Fcitx(appContext) }

    private fun mkConnection(name: String) = object : FcitxConnection {

        private inline fun <T> ensureConnected(block: () -> T) =
            if (name in clients)
                block()
            else throw IllegalStateException("$name is disconnected")

        override fun <T> runImmediately(block: suspend FcitxAPI.() -> T): T = ensureConnected {
            runBlocking(realFcitx.lifeCycleScope.coroutineContext) {
                block(realFcitx)
            }
        }

        override suspend fun <T> runOnReady(block: suspend FcitxAPI.() -> T): T = ensureConnected {
            realFcitx.lifecycle.whenReady { block(realFcitx) }
        }

        override suspend fun runIfReady(block: suspend FcitxAPI.() -> Unit) {
            ensureConnected {
                if (realFcitx.isReady)
                    realFcitx.lifeCycleScope.launch {
                        block(realFcitx)
                    }
            }
        }

    }

    private val lock = ReentrantLock()

    private val clients = mutableMapOf<String, FcitxConnection>()

    fun connect(name: String): FcitxConnection = lock.withLock {
        if (name in clients)
            return@withLock clients.getValue(name)
        if (realFcitx.lifecycle.currentState == FcitxLifecycle.State.STOPPED) {
            Timber.d("Start fcitx")
            realFcitx.start()
        }
        val new = mkConnection(name)
        clients[name] = new
        return@withLock new
    }

    fun disconnect(name: String): Unit = lock.withLock {
        if (name !in clients)
            return
        clients -= name
        if (clients.isEmpty()) {
            Timber.d("Stop fcitx")
            realFcitx.stop()
        }
    }

}