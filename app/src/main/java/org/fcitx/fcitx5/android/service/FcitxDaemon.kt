package org.fcitx.fcitx5.android.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.FcitxLifecycle
import org.fcitx.fcitx5.android.core.FcitxLifecycleObserver
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

class FcitxDaemon : LifecycleService(), FcitxLifecycleObserver {

    val fcitx: Fcitx by lazy { Fcitx(this).also { it.lifecycle.addObserver(this) } }

    private val onReadyListeners = ConcurrentLinkedQueue<suspend () -> Unit>()
    private val onStoppedListeners = ConcurrentLinkedQueue<suspend () -> Unit>()

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()
    }

    inner class FcitxBinder : Binder() {
        fun getFcitxDaemon() = this@FcitxDaemon

        fun onReady(block: suspend () -> Unit) {
            // if fcitx is ready, call right now
            if (fcitx.lifecycle.currentState == FcitxLifecycle.State.READY)
                lifecycleScope.launch { block() }
            // always save it in case fcitx restarted
            onReadyListeners.add(block)
        }

        fun onStopped(block: suspend () -> Unit) {
            // if fcitx is stopped, call right now
            if (fcitx.lifecycle.currentState == FcitxLifecycle.State.STOPPED)
                lifecycleScope.launch { block() }
            // always save it in case fcitx restarted
            onStoppedListeners.add(block)
        }
    }

    override fun onStateChanged(event: FcitxLifecycle.Event) {
        when (event) {
            FcitxLifecycle.Event.ON_READY -> onReadyListeners.forEach { lifecycleScope.launch { it() } }
            FcitxLifecycle.Event.ON_STOPPED -> onStoppedListeners.forEach { lifecycleScope.launch { it() } }
            else -> {
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Timber.d("onBind")
        fcitx.start()
        return FcitxBinder()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        runBlocking {
            fcitx.save()
        }
        fcitx.stop()
        onReadyListeners.clear()
        onStoppedListeners.clear()
        super.onDestroy()
    }
}
