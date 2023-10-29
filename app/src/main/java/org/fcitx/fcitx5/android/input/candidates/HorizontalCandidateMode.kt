/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class HorizontalCandidateMode {
    NeverFillWidth,
    AutoFillWidth,
    AlwaysFillWidth;

    companion object : ManagedPreference.StringLikeCodec<HorizontalCandidateMode> {
        override fun decode(raw: String): HorizontalCandidateMode = valueOf(raw)
    }
}