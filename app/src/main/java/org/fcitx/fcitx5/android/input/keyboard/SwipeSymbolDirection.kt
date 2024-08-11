/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class SwipeSymbolDirection(override val stringRes: Int): ManagedPreferenceEnum {
    Up(R.string.swipe_up),
    Down(R.string.swipe_down),
    Disabled(R.string.disabled);

    fun checkY(totalY: Int): Boolean =
        (this != Disabled) && (totalY != 0) && ((totalY > 0) == (this == Down))
}
