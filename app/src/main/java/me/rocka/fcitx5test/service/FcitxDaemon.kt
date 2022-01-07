package me.rocka.fcitx5test.service

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
        Log.d(javaClass.name, "FcitxDaemon onBind")
        fcitx.start()
        return FcitxBinder()
    }

    override fun onDestroy() {
        Log.d(javaClass.name, "FcitxDaemon onDestroy")
        fcitx.stop()
        leftoverOnReadyListeners.clear()
    }
}
