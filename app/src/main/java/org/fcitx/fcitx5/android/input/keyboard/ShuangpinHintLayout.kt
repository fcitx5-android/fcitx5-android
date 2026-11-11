/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class ShuangpinHintLayout(override val stringRes: Int) : ManagedPreferenceEnum {
    Split(R.string.shuangpin_hint_layout_split),
    Above(R.string.shuangpin_hint_layout_above),
    Below(R.string.shuangpin_hint_layout_below),
    AboveWithSymbols(R.string.shuangpin_hint_layout_above_symbols_below),
    BelowWithSymbols(R.string.shuangpin_hint_layout_symbols_above_below)
}
