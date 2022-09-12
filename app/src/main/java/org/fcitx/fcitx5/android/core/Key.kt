package org.fcitx.fcitx5.android.core

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Key(
    val sym: Int,
    val states: Int,
    val portableString: String,
    val localizedString: String
) : Parcelable {
    @IgnoredOnParcel
    val keySym by lazy { KeySym.of(sym) }

    @IgnoredOnParcel
    val keyStates by lazy { KeyStates.of(states) }

    companion object {
        val None = Key( 0, 0, "", "")

        @JvmStatic
        external fun parse(raw: String): Key

        @JvmStatic
        private external fun create(sym: Int, states: Int): Key

        @JvmStatic
        fun create(sym: KeySym, states: KeyStates): Key = create(sym.toInt(), states.toInt())
    }
}
