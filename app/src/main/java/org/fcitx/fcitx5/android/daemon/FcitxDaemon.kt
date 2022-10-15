package org.fcitx.fcitx5.android.daemon

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.*
import org.fcitx.fcitx5.android.utils.appContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object FcitxDaemon {

    private val realFcitx by lazy { Fcitx(appContext) }

    private val connection = object : FcitxConnection {

        override fun <T> runImmediately(block: suspend FcitxAPI.() -> T): T =
            runBlocking(realFcitx.lifeCycleScope.coroutineContext) {
                block(realFcitx)
            }

        override suspend fun <T> runOnReady(block: suspend FcitxAPI.() -> T): T =
            realFcitx.lifecycle.whenReady { block(realFcitx) }

        override suspend fun runIfReady(block: suspend FcitxAPI.() -> Unit) {
            realFcitx.lifeCycleScope.launch {
                block(realFcitx)
            }
        }

    }

    private val lock = ReentrantLock()

    private val clients = mutableSetOf<String>()

    fun connect(name: String): FcitxConnection = lock.withLock {
        if (name in clients)
            return@withLock connection
        if (realFcitx.lifecycle.currentState == FcitxLifecycle.State.STOPPED)
            realFcitx.start()
        clients += name
        return@withLock connection
    }

    fun disconnect(name: String): Unit = lock.withLock {
        if (name !in clients)
            return
        clients -= name
        if (clients.isEmpty())
            realFcitx.stop()
    }

}