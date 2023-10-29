/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class ExpandedCandidateStyle {
    Grid,
    Flexbox;

    companion object : ManagedPreference.StringLikeCodec<ExpandedCandidateStyle> {
        override fun decode(raw: String): ExpandedCandidateStyle = valueOf(raw)
    }
}