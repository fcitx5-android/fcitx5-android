package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym

object KeyUtils {
    @JvmStatic
    external fun parseKey(raw: String): Key

    @JvmStatic
    private external fun createKey(sym: Int, states: Int): Key

    @JvmStatic
    fun createKey(sym: KeySym, states: KeyStates): Key = createKey(sym.toInt(), states.toInt())
}