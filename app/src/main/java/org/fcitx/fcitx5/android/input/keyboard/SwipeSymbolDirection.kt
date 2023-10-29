/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class SwipeSymbolDirection {
    Up,
    Down,
    Disabled;

    fun checkY(totalY: Int): Boolean =
        (this != Disabled) && (totalY != 0) && ((totalY > 0) == (this == Down))

    companion object : ManagedPreference.StringLikeCodec<SwipeSymbolDirection> {
        override fun decode(raw: String): SwipeSymbolDirection = valueOf(raw)
    }
}
