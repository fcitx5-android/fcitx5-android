package org.fcitx.fcitx5.android.utils

import com.sun.jna.Library
import com.sun.jna.Structure
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym

interface KeyUtils : Library {
    @Structure.FieldOrder("successful", "str", "sym", "states")
    class parsed_key : Structure(), Structure.ByValue {
        @JvmField
        var successful: Boolean = false

        @JvmField
        var str: String = ""

        @JvmField
        var sym: Int = 0

        @JvmField
        var states: Int = 0

        override fun toString(): String = str
        val keySym: KeySym by lazy { KeySym.of(sym) }
        val keyStates: KeyStates by lazy { KeyStates.of(sym) }
    }

    fun parse_key(raw: String): parsed_key

    companion object {
        val instance: KeyUtils by nativeLib("key-utils")
    }
}