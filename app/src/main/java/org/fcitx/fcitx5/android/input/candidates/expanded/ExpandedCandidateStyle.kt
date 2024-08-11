/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class ExpandedCandidateStyle(override val stringRes: Int) : ManagedPreferenceEnum {
    Grid(R.string.expanded_candidate_style_grid),
    Flexbox(R.string.expanded_candidate_style_flexbox);
}
