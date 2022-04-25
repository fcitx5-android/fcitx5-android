package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceCategory

object ThemeManager {

    class Prefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceCategory(R.string.theme, sharedPreferences) {

        val keyBorder = switch(R.string.key_border, "key_border", false)

        val keyRippleEffect = switch(R.string.key_ripple_effect, "key_ripple_effect", true)

        val keyHorizontalMargin =
            int(R.string.key_horizontal_margin, "key_horizontal_margin", 3, 0, 8)

        val keyVerticalMargin = int(R.string.key_vertical_margin, "key_vertical_margin", 7, 0, 16)

        val keyRadius = int(R.string.key_radius, "key_radius", 4, 0, 48)

    }

    val prefs = AppPrefs.getInstance().registerProvider(::Prefs)

    // TODO
    fun getAllThemes() = builtinThemes

    private val builtinThemes = listOf(
        ThemePreset.PixelLight,
        ThemePreset.PreviewDark
    )

    // TODO
    val currentTheme: Theme = ThemePreset.PixelDark
}