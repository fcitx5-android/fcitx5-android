package org.fcitx.fcitx5.android.daemon

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.*
import org.fcitx.fcitx5.android.daemon.FcitxDaemon.connect
import org.fcitx.fcitx5.android.daemon.FcitxDaemon.disconnect
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manage the singleton instance of [Fcitx]
 *
 * To use fcitx, client should call [connect] to obtain a [FcitxConnection],
 * and call [disconnect] on client destroyed. Client should not leak the instance of [FcitxAPI],
 * and must use [FcitxConnection] to access fcitx functionalities.
 *
 * The instance of [Fcitx] always exists,but whether the dispatcher runs and callback works depend on clients, i.e.
 * if no clients are connected, [Fcitx.stop] will be called.
 *
 * Functions are thread-safe in this class.
 */
object FcitxDaemon {

    private val realFcitx by lazy { Fcitx(appContext) }

    // don't leak fcitx instance
    private val fcitxImpl by lazy { object : FcitxAPI by realFcitx {} }

    private fun mkConnection(name: String) = object : FcitxConnection {

        private inline fun <T> ensureConnected(block: () -> T) =
            if (name in clients)
                block()
            else throw IllegalStateException("$name is disconnected")

        override fun <T> runImmediately(block: suspend FcitxAPI.() -> T): T = ensureConnected {
            runBlocking(realFcitx.lifeCycleScope.coroutineContext) {
                block(fcitxImpl)
            }
        }

        override suspend fun <T> runOnReady(block: suspend FcitxAPI.() -> T): T = ensureConnected {
            realFcitx.lifecycle.whenReady { block(fcitxImpl) }
        }

        override fun runIfReady(block: suspend FcitxAPI.() -> Unit) {
            ensureConnected {
                if (realFcitx.isReady)
                    realFcitx.lifeCycleScope.launch {
                        block(fcitxImpl)
                    }
            }
        }

    }

    private val lock = ReentrantLock()

    private val clients = mutableMapOf<String, FcitxConnection>()

    /**
     * Create a connection
     */
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

    /**
     * Dispose the connection
     */
    fun disconnect(name: String): Unit = lock.withLock {
        if (name !in clients)
            return
        clients -= name
        if (clients.isEmpty()) {
            Timber.d("Stop fcitx")
            realFcitx.stop()
        }
    }

    /**
     * Restart fcitx instance while keep the clients connected
     */
    fun restartFcitx() = lock.withLock {
        realFcitx.stop()
        realFcitx.start()
    }

}