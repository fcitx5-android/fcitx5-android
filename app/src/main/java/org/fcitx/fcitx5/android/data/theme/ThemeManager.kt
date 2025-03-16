/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.theme.ThemeManager.activeTheme
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.isDarkMode

object ThemeManager {

    fun interface OnThemeChangeListener {
        fun onThemeChange(theme: Theme)
    }

    val BuiltinThemes = listOf(
        ThemePreset.MaterialLight,
        ThemePreset.MaterialDark,
        ThemePreset.PixelLight,
        ThemePreset.PixelDark,
        ThemePreset.NordLight,
        ThemePreset.NordDark,
        ThemePreset.DeepBlue,
        ThemePreset.Monokai,
        ThemePreset.AMOLEDBlack,
    )

    val DefaultTheme = ThemePreset.PixelDark

    private var monetThemes = listOf(ThemeMonet.getLight(), ThemeMonet.getDark())

    private val customThemes: MutableList<Theme.Custom> = ThemeFilesManager.listThemes()

    fun getTheme(name: String) =
        customThemes.find { it.name == name } ?: BuiltinThemes.find { it.name == name }

    fun getAllThemes() = customThemes + monetThemes + BuiltinThemes

    fun refreshThemes() {
        customThemes.clear()
        customThemes.addAll(ThemeFilesManager.listThemes())
        activeTheme = evaluateActiveTheme()
    }

    /**
     * [backing property](https://kotlinlang.org/docs/properties.html#backing-properties)
     * of [activeTheme]; holds the [Theme] object currently in use
     */
    private lateinit var _activeTheme: Theme

    var activeTheme: Theme
        get() = _activeTheme
        private set(value) {
            if (_activeTheme == value) return
            _activeTheme = value
            fireChange()
        }

    private var isDarkMode = false

    private val onChangeListeners = WeakHashSet<OnThemeChangeListener>()

    fun addOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.add(listener)
    }

    fun removeOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.remove(listener)
    }

    private fun fireChange() {
        onChangeListeners.forEach { it.onThemeChange(_activeTheme) }
    }

    val prefs = AppPrefs.getInstance().registerProvider(::ThemePrefs)

    fun saveTheme(theme: Theme.Custom) {
        ThemeFilesManager.saveThemeFiles(theme)
        customThemes.indexOfFirst { it.name == theme.name }.also {
            if (it >= 0) customThemes[it] = theme else customThemes.add(0, theme)
        }
        if (activeTheme.name == theme.name) {
            activeTheme = theme
        }
    }

    fun deleteTheme(name: String) {
        customThemes.find { it.name == name }?.also {
            ThemeFilesManager.deleteThemeFiles(it)
            customThemes.remove(it)
        }
        if (activeTheme.name == name) {
            activeTheme = evaluateActiveTheme()
        }
    }

    fun setNormalModeTheme(theme: Theme) {
        // `normalModeTheme.setValue(theme)` would trigger `onThemePrefsChange` listener,
        // which calls `fireChange()`.
        // `activateTheme`'s setter would also trigger `fireChange()` when theme actually changes.
        // write to backing property directly to avoid unnecessary `fireChange()`
        _activeTheme = theme
        prefs.normalModeTheme.setValue(theme)
    }

    private fun evaluateActiveTheme(): Theme {
        return if (prefs.followSystemDayNightTheme.getValue()) {
            if (isDarkMode) prefs.darkModeTheme else prefs.lightModeTheme
        } else {
            prefs.normalModeTheme
        }.getValue()
    }

    @Keep
    private val onThemePrefsChange = ManagedPreferenceProvider.OnChangeListener { key ->
        if (prefs.dayNightModePrefNames.contains(key)) {
            activeTheme = evaluateActiveTheme()
        } else {
            fireChange()
        }
    }

    fun init(configuration: Configuration) {
        isDarkMode = configuration.isDarkMode()
        // fire all `OnThemeChangedListener`s on theme preferences change
        prefs.registerOnChangeListener(onThemePrefsChange)
        _activeTheme = evaluateActiveTheme()
    }

    fun onSystemPlatteChange(newConfig: Configuration) {
        isDarkMode = newConfig.isDarkMode()
        monetThemes = listOf(ThemeMonet.getLight(), ThemeMonet.getDark())
        // `ManagedThemePreference` finds a theme with same name in `getAllThemes()`
        // thus `evaluateActiveTheme()` should be called after updating `monetThemes`
        activeTheme = evaluateActiveTheme()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun syncToDeviceEncryptedStorage() {
        val ctx = appContext.createDeviceProtectedStorageContext()
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        sp.edit {
            prefs.managedPreferences.forEach {
                it.value.putValueTo(this@edit)
            }
        }
    }

}