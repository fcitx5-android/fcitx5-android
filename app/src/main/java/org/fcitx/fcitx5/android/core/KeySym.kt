/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import android.view.KeyEvent

@JvmInline
value class KeySym(val sym: Int) {

    val keyCode get() = FcitxKeyMapping.symToKeyCode(sym) ?: KeyEvent.KEYCODE_UNKNOWN

    override fun toString() = "0x" + sym.toString(16).padStart(4, '0')

    companion object {
        fun fromKeyEvent(event: KeyEvent) =
            FcitxKeyMapping.keyCodeToSym(event.keyCode)?.let { KeySym(it) }
    }
}