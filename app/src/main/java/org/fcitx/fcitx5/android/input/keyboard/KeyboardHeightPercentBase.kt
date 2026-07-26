/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class KeyboardHeightPercentBase(override val stringRes: Int) : ManagedPreferenceEnum {
    DisplayMetrics(R.string.display_metrics),
    RealSize(R.string.real_size)
}
