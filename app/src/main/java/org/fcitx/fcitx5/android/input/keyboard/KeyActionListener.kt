/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

fun interface KeyActionListener {

    enum class Source {
        Keyboard, Popup
    }

    fun onKeyAction(action: KeyAction, source: Source)
}
