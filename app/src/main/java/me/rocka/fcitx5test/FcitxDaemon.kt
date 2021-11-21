package me.rocka.fcitx5test

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxLifecycleObserver
import java.util.concurrent.atomic.AtomicInteger

class FcitxDaemon : Service() {
    private val fcitx = Fcitx(this).apply {
        observer = object : FcitxLifecycleObserver {
            override fun onReady() {
                Log.d(javaClass.name, "FcitxDaemon onReady")
            }

            override fun onStopped() {
                Log.d(javaClass.name, "FcitxDaemon onStopped")
            }

        }
    }
    private val bindingCount = AtomicInteger(0)

    override fun onCreate() {
        Log.d(javaClass.name, "FcitxDaemon onCreate")
        super.onCreate()
    }

    inner class MyBinder : Binder() {
        fun getFcitxInstance() = fcitx
    }

    override fun onBind(intent: Intent?): IBinder {
        bindingCount.getAndIncrement()
        Log.d(javaClass.name, "FcitxDaemon onBind, count = ${bindingCount.get()}")
        fcitx.start()
        return MyBinder()
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
    }
}