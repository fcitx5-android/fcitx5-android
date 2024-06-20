/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceCategory

class ThemePrefs(sharedPreferences: SharedPreferences) :
    ManagedPreferenceCategory(R.string.theme, sharedPreferences) {

    private fun themePreference(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Theme,
        @StringRes
        summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedThemePreference {
        val pref = ManagedThemePreference(sharedPreferences, key, defaultValue)
        val ui = ManagedThemePreferenceUi(title, key, defaultValue, summary, enableUiOn)
        pref.register()
        ui.registerUi()
        return pref
    }

    val keyBorder = switch(R.string.key_border, "key_border", false)

    val keySymbolBorder = switch(R.string.key_symbol_border, "key_symbol_border", false)

    val keyRippleEffect = switch(R.string.key_ripple_effect, "key_ripple_effect", false)

    val keyHorizontalMargin: ManagedPreference.PInt
    val keyHorizontalMarginLandscape: ManagedPreference.PInt

    init {
        val (primary, secondary) = twinInt(
            R.string.key_horizontal_margin,
            R.string.portrait,
            "key_horizontal_margin",
            3,
            R.string.landscape,
            "key_horizontal_margin_landscape",
            3,
            0,
            24,
            "dp"
        )
        keyHorizontalMargin = primary
        keyHorizontalMarginLandscape = secondary
    }

    val keyVerticalMargin: ManagedPreference.PInt
    val keyVerticalMarginLandscape: ManagedPreference.PInt

    init {
        val (primary, secondary) = twinInt(
            R.string.key_vertical_margin,
            R.string.portrait,
            "key_vertical_margin",
            7,
            R.string.landscape,
            "key_vertical_margin_landscape",
            4,
            0,
            24,
            "dp"
        )
        keyVerticalMargin = primary
        keyVerticalMarginLandscape = secondary
    }

    val keyRadius = int(R.string.key_radius, "key_radius", 4, 0, 48, "dp")

    enum class KeyTextEditingStyle {
        Linear,
        Border,
        Borderless;

        companion object : ManagedPreference.StringLikeCodec<KeyTextEditingStyle> {
            override fun decode(raw: String): KeyTextEditingStyle = valueOf(raw)
        }
    }

    val keyTextEditingStyle = list(
        R.string.key_text_editing_style,
        "key_text_editing_style",
        KeyTextEditingStyle.Linear,
        KeyTextEditingStyle,
        listOf(
            KeyTextEditingStyle.Linear,
            KeyTextEditingStyle.Border,
            KeyTextEditingStyle.Borderless,
        ),
        listOf(
            R.string.key_text_editing_style_linear,
            R.string.key_text_editing_style_border,
            R.string.key_text_editing_style_borderless
        )
    )

    val keyTextEditingRippleEffect =
        switch(R.string.key_text_editing_ripple_effect, "key_text_editing_ripple_effect", false)

    val keyTextEditingMargin: ManagedPreference.PInt
    val keyTextEditingMarginLandscape: ManagedPreference.PInt

    init {
        val (primary, secondary) = twinInt(
            R.string.key_text_editing_margin,
            R.string.portrait,
            "key_text_editing_margin",
            4,
            R.string.landscape,
            "key_text_editing_margin_landscape",
            4,
            0,
            24,
            "dp"
        )
        keyTextEditingMargin = primary
        keyTextEditingMarginLandscape = secondary
    }

    val keyTextEditingRadius =
        int(R.string.key_text_editing_radius, "key_text_editing_radius", 4, 0, 48, "dp")

    val clipboardContentRadius =
        int(R.string.clipboard_content_radius, "clipboard_content_radius", 2, 0, 48, "dp")

    enum class PunctuationPosition {
        Bottom,
        TopRight;

        companion object : ManagedPreference.StringLikeCodec<PunctuationPosition> {
            override fun decode(raw: String): PunctuationPosition = valueOf(raw)
        }
    }

    val punctuationPosition = list(
        R.string.punctuation_position,
        "punctuation_position",
        PunctuationPosition.Bottom,
        PunctuationPosition,
        listOf(
            PunctuationPosition.Bottom,
            PunctuationPosition.TopRight
        ),
        listOf(
            R.string.punctuation_pos_bottom,
            R.string.punctuation_pos_top_right
        )
    )

    enum class NavbarBackground {
        None,
        ColorOnly,
        Full;

        companion object : ManagedPreference.StringLikeCodec<NavbarBackground> {
            override fun decode(raw: String): NavbarBackground = valueOf(raw)
        }
    }

    val navbarBackground = list(
        R.string.navbar_background,
        "navbar_background",
        NavbarBackground.ColorOnly,
        NavbarBackground,
        listOf(
            NavbarBackground.None,
            NavbarBackground.ColorOnly,
            NavbarBackground.Full
        ),
        listOf(
            R.string.navbar_bkg_none,
            R.string.navbar_bkg_color_only,
            R.string.navbar_bkg_full
        )
    )

    /**
     * When [followSystemDayNightTheme] is disabled, this theme is used.
     * This is effectively an internal preference which does not need UI.
     */
    val normalModeTheme = ManagedThemePreference(
        sharedPreferences, "normal_mode_theme", ThemeManager.DefaultTheme
    ).also {
        it.register()
    }

    val followSystemDayNightTheme = switch(
        R.string.follow_system_day_night_theme,
        "follow_system_dark_mode",
        false,
        summary = R.string.follow_system_day_night_theme_summary
    )

    val lightModeTheme = themePreference(
        R.string.light_mode_theme,
        "light_mode_theme",
        ThemePreset.PixelLight,
        enableUiOn = {
            followSystemDayNightTheme.getValue()
        })

    val darkModeTheme = themePreference(
        R.string.dark_mode_theme,
        "dark_mode_theme",
        ThemePreset.PixelDark,
        enableUiOn = {
            followSystemDayNightTheme.getValue()
        })

    val dayNightModePrefNames = setOf(
        followSystemDayNightTheme.key,
        lightModeTheme.key,
        darkModeTheme.key
    )
}
