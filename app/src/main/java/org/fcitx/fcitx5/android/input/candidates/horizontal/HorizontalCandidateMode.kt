/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.horizontal

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class HorizontalCandidateMode(override val stringRes: Int) : ManagedPreferenceEnum {
    NeverFillWidth(R.string.horizontal_candidate_never_fill),
    AutoFillWidth(R.string.horizontal_candidate_auto_fill),
    AlwaysFillWidth(R.string.horizontal_candidate_always_fill);
}
