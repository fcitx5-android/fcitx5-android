package me.rocka.fcitx5test

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxLifecycleObserver
import me.rocka.fcitx5test.native.FcitxState
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class FcitxDaemon : Service(), CoroutineScope by MainScope() {
    private val fcitx = Fcitx(this).apply {
        observer = object : FcitxLifecycleObserver {
            override fun onReady() {
                Log.d(javaClass.name, "FcitxDaemon onReady")
                launch {
                    while (leftoverOnReadyListeners.isNotEmpty())
                        leftoverOnReadyListeners.remove()()
                }
            }

            override fun onStopped() {
                Log.d(javaClass.name, "FcitxDaemon onStopped")
            }

        }
    }
    private val bindingCount = AtomicInteger(0)

    private val leftoverOnReadyListeners = ConcurrentLinkedQueue<() -> Unit>()

    override fun onCreate() {
        Log.d(javaClass.name, "FcitxDaemon onCreate")
        super.onCreate()
    }

    inner class FcitxBinder : Binder() {
        fun getFcitxInstance() = fcitx

        fun onReady(block: () -> Unit) {
            // if fcitx is ready, call right now
            if (fcitx.currentState == FcitxState.Ready)
                launch { block() }
            // otherwise save it
            else leftoverOnReadyListeners.add(block)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        bindingCount.getAndIncrement()
        Log.d(javaClass.name, "FcitxDaemon onBind, count = ${bindingCount.get()}")
        fcitx.start()
        return FcitxBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(javaClass.name, "FcitxDaemon onUnbind, count = ${bindingCount.get()}")
        if (bindingCount.decrementAndGet() == 0)
            fcitx.stop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(javaClass.name, "FcitxDaemon onDestroy")
        fcitx.stop()
        leftoverOnReadyListeners.clear()
    }
}
