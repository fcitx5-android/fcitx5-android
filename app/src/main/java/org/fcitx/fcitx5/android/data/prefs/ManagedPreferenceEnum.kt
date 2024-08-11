/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.data.prefs

import androidx.annotation.StringRes

interface ManagedPreferenceEnum {
    @get:StringRes
    val stringRes: Int
}
