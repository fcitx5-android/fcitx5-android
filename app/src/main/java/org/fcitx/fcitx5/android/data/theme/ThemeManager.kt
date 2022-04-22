package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceInternal

object ThemeManager {

    class Prefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceInternal(sharedPreferences) {

        val keyBorder = bool("key_border", false)

        val keyRippleEffect = bool("key_ripple_effect", true)

        val keyHorizontalMargin = int("key_horizontal_margin", 3)

        val keyVerticalMargin = int("key_vertical_margin", 7)

        val keyRadius = float("key_radius", 4f)

    }

    val prefs = AppPrefs.getInstance().registerProvider(::Prefs)

    // TODO
    val currentTheme: Theme = ThemePreset.test
}