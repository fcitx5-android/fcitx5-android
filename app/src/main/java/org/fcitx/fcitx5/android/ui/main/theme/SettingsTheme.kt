/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.theme

import android.content.Context
import android.os.Build
import androidx.annotation.StyleRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum

enum class SettingsTheme(override val stringRes: Int, @get:StyleRes val activityTheme: Int) : ManagedPreferenceEnum {
    MaterialDesign(R.string.settings_theme_material, R.style.Theme_FcitxAppTheme),
    Material3(R.string.settings_theme_material3, R.style.Theme_FcitxSettings_Material3);

    companion object {
        fun current() = AppPrefs.getInstance().advanced.settingsTheme.getValue()
    }
}

internal fun systemSettingsColorScheme(context: Context, dark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return try {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } catch (_: RuntimeException) {
        // Some system images expose the API without the complete Monet resources.
        null
    }
}

internal fun settingsColorScheme(context: Context, dark: Boolean): ColorScheme =
    systemSettingsColorScheme(context, dark) ?: defaultSettingsColorScheme(dark)

internal fun defaultSettingsColorScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = Color(0xffaac7ff),
            background = Color(0xff111318),
            surface = Color(0xff111318),
            onSurface = Color(0xffe1e2e9)
        )
    } else {
        lightColorScheme(
            primary = Color(0xff315da8),
            background = Color(0xfff9f9ff),
            surface = Color(0xfff9f9ff),
            onSurface = Color(0xff191c20)
        )
    }

@Composable
fun SettingsThemeContent(content: @Composable () -> Unit) {
    val colors = settingsColorScheme(LocalContext.current, isSystemInDarkTheme())
    MaterialTheme(colorScheme = colors, content = content)
}
