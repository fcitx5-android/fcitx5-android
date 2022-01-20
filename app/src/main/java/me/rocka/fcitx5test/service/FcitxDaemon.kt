package me.rocka.fcitx5test.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.native.Fcitx
import java.util.concurrent.ConcurrentLinkedQueue

class FcitxDaemon : LifecycleService() {
    private val fcitx by lazy {
        Fcitx(this).also {
            lifecycleScope.launch {
                it.lifecycle.whenStarted {
                    while (leftoverOnReadyListeners.isNotEmpty())
                        leftoverOnReadyListeners.remove()()
                }
            }
        }
    }

    private val leftoverOnReadyListeners = ConcurrentLinkedQueue<() -> Unit>()

    override fun onCreate() {
        Log.d(javaClass.name, "FcitxDaemon onCreate")
        super.onCreate()
    }

    inner class FcitxBinder : Binder() {
        fun getFcitxInstance() = fcitx

        fun onReady(block: () -> Unit) {
            // if fcitx is ready, call right now
            if (fcitx.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                lifecycleScope.launch { block() }
            // otherwise save it
            else leftoverOnReadyListeners.add(block)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(javaClass.name, "FcitxDaemon onBind")
        fcitx.start()
        return FcitxBinder()
    }

    override fun onDestroy() {
        Log.d(javaClass.name, "FcitxDaemon onDestroy")
        fcitx.stop()
        leftoverOnReadyListeners.clear()
        super.onDestroy()
    }
}
