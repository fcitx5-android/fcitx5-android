package org.fcitx.fcitx5.android.core

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Key(
    val keyString: String,
    val sym: Int,
    val states: Int
) : Parcelable {
    @IgnoredOnParcel
    val keySym by lazy { KeySym.of(sym) }

    @IgnoredOnParcel
    val keyStates by lazy { KeyStates.of(states) }

    companion object {
        val none = Key("", 0, 0)
    }

    fun showKeyString() = keyString.takeIf { it.isNotEmpty() } ?: "None"
}
