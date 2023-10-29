/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

enum class SpaceLongPressBehavior {
    None,
    Enumerate,
    ToggleActivate,
    ShowPicker;

    companion object : ManagedPreference.StringLikeCodec<SpaceLongPressBehavior> {
        override fun decode(raw: String): SpaceLongPressBehavior = valueOf(raw)
    }
}
