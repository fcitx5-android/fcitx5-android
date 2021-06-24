package me.rocka.fcitx5test.native

import android.util.Log
import org.greenrobot.eventbus.EventBus

object JNI {

    init {
        System.loadLibrary("native-lib")
    }

    external fun startupFcitx(appData: String, appLib: String, extData: String): Int

    external fun exitFcitx()

    external fun sendKeyToFcitx(key: String)

    external fun sendKeyToFcitx(c: Char)

    external fun selectCandidate(idx: Int)

    /**
     * Called from native-lib
     */
    @Suppress("unused")
    private fun handleFcitxEvent(type: Int, vararg params: Any) {
        EventBus.getDefault().post(FcitxEvent.create(type, params.asList()))
        Log.d("FcitxEvent", "type=${type}, params=${params.run { "[$size]" + joinToString(",") }}")
    }

}