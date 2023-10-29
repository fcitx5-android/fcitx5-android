/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import androidx.core.content.edit
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

class ManagedThemePreference(
    sharedPreferences: SharedPreferences,
    key: String,
    defaultValue: Theme,
) : ManagedPreference<Theme>(
    sharedPreferences, key, defaultValue
) {

    override fun setValue(value: Theme) {
        sharedPreferences.edit { putString(key, value.name) }
    }

    override fun getValue(): Theme =
        sharedPreferences.getString(key, null)?.let { name ->
            ThemeManager.getAllThemes().find { it.name == name }
        } ?: defaultValue

    override fun putValueTo(editor: SharedPreferences.Editor) {
        editor.putString(key, getValue().name)
    }

}