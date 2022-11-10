package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class SwipeSymbolDirection {
    Up,
    Down,
    Disabled;

    fun checkY(totalY: Int): Boolean = (totalY != 0) && ((totalY > 0) == (this == Down))

    companion object : ManagedPreference.StringLikeCodec<SwipeSymbolDirection> {
        override fun decode(raw: String): SwipeSymbolDirection = valueOf(raw)
    }
}
