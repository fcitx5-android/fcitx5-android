package me.rocka.fcitx5test.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rocka.fcitx5test.native.*
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

class FcitxDaemon : LifecycleService(), FcitxLifecycleObserver {

    val fcitx: Fcitx by lazy { Fcitx(this).also { it.lifecycle.addObserver(this) } }

    private val onReadyListeners = ConcurrentLinkedQueue<() -> Unit>()
    private val onStoppedListeners = ConcurrentLinkedQueue<() -> Unit>()

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()
    }

    inner class FcitxBinder : Binder() {
        fun getFcitxDaemon() = this@FcitxDaemon

        fun onReady(block: () -> Unit) {
            // if fcitx is ready, call right now
            if (fcitx.lifecycle.currentState == FcitxLifecycle.State.READY)
                lifecycleScope.launch { block() }
            // always save it in case fcitx restarted
            onReadyListeners.add(block)
        }

        fun onStopped(block: () -> Unit) {
            // if fcitx is stopped, call right now
            if (fcitx.lifecycle.currentState == FcitxLifecycle.State.STOPPED)
                lifecycleScope.launch { block() }
            // always save it in case fcitx restarted
            onStoppedListeners.add(block)
        }
    }

    override fun onStateChanged(event: FcitxLifecycle.Event) {
        when (event) {
            FcitxLifecycle.Event.ON_READY -> lifecycleScope.launch { onReadyListeners.forEach { it() } }
            FcitxLifecycle.Event.ON_STOPPED -> lifecycleScope.launch { onStoppedListeners.forEach { it() } }
            else -> {
            }
        }
    }

    suspend fun restartFcitx() = withContext(Dispatchers.IO) {
        fcitx.stop()
        fcitx.start()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Timber.d("onBind")
        fcitx.start()
        return FcitxBinder()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        fcitx.stop()
        onReadyListeners.clear()
        onStoppedListeners.clear()
        super.onDestroy()
    }
}
