/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.content.ClipData
import android.os.Build

fun ClipData.timestamp() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    description.timestamp
} else {
    System.currentTimeMillis()
}
