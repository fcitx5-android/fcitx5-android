/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.floating

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class FloatingCandidatesOrientation(override val stringRes: Int): ManagedPreferenceEnum {
    Automatic(R.string.automatic),
    Horizontal(R.string.horizontal),
    Vertical(R.string.vertical)
}
