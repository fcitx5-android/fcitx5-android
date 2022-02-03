package me.rocka.fcitx5test.core

import com.sun.jna.Callback
import com.sun.jna.Library
import me.rocka.fcitx5test.utils.nativeLib

@Suppress("FunctionName", "ClassName")
interface NativeLib : Library {
    fun interface log_callback_t : Callback {
        operator fun invoke(log: String)
    }

    fun setup_log_stream(callback: log_callback_t)

    companion object {
        val instance: NativeLib by nativeLib("native-lib")
    }
}