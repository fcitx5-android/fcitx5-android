/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.provider.Settings

fun isSystemSettingEnabled(key: String): Boolean {
    return try {
        Settings.System.getInt(appContext.contentResolver, key) == 1
    } catch (e: Exception) {
        false
    }
}

fun getSecureSettings(name: String): String? {
    return Settings.Secure.getString(appContext.contentResolver, name)
}
