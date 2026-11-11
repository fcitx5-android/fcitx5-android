/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.theme

import android.os.Bundle
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

abstract class SettingsActivity : AppCompatActivity() {
    private var appliedTheme: SettingsTheme? = null
    private val preference get() = AppPrefs.getInstance().advanced.settingsTheme
    private val listener = ManagedPreference.OnChangeListener<SettingsTheme> { _, theme ->
        if (theme != appliedTheme && !isFinishing && !isDestroyed) {
            window.decorView.post { if (!isFinishing && !isDestroyed) recreate() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = preference.getValue()
        setTheme(appliedTheme!!.activityTheme)
        super.onCreate(savedInstanceState)
        if (appliedTheme == SettingsTheme.Material3) {
            val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            if (systemSettingsColorScheme(this, dark) != null) {
                try {
                    theme.applyStyle(com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight, true)
                } catch (_: RuntimeException) {
                    setTheme(appliedTheme!!.activityTheme)
                }
            }
        }
        preference.registerOnChangeListener(listener)
    }

    override fun onResume() {
        super.onResume()
        if (appliedTheme != preference.getValue()) recreate()
    }

    override fun onDestroy() {
        preference.unregisterOnChangeListener(listener)
        super.onDestroy()
    }
}
