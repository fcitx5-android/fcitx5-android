/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.view.WindowCompat
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePrefs.NavbarBackground

class NavigationBarManager {

    private val keyBorder by ThemeManager.prefs.keyBorder
    private val navbarBackground by ThemeManager.prefs.navbarBackground

    private var shouldUpdateNavbarForeground = false
    private var shouldUpdateNavbarBackground = false

    private fun Window.useSystemNavbarBackground(enabled: Boolean) {
        // 35+ enforces edge to edge and we must draw behind navbar
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowCompat.setDecorFitsSystemWindows(this, enabled)
        }
    }

    private fun Window.setNavbarBackgroundColor(@ColorInt color: Int) {
        /**
         * Why on earth does it deprecated? It says
         * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r3/core/java/android/view/Window.java#2720
         * "If the app targets VANILLA_ICE_CREAM or above, the color will be transparent and cannot be changed"
         * but it only takes effect on API 35+ devices. Older devices still needs this.
         */
        @Suppress("DEPRECATION")
        navigationBarColor = color
    }

    private fun Window.enforceNavbarContrast(enforced: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isNavigationBarContrastEnforced = enforced
        }
    }

    fun evaluate(window: Window) {
        when (navbarBackground) {
            NavbarBackground.None -> {
                shouldUpdateNavbarForeground = false
                shouldUpdateNavbarBackground = false
                window.useSystemNavbarBackground(true)
                window.enforceNavbarContrast(true)
            }
            NavbarBackground.ColorOnly -> {
                shouldUpdateNavbarForeground = true
                shouldUpdateNavbarBackground = true
                window.useSystemNavbarBackground(true)
                window.enforceNavbarContrast(false)
            }
            NavbarBackground.Full -> {
                shouldUpdateNavbarForeground = true
                shouldUpdateNavbarBackground = false
                window.useSystemNavbarBackground(false)
                window.setNavbarBackgroundColor(Color.TRANSPARENT)
                window.enforceNavbarContrast(false)
            }
        }
    }

    fun evaluate(window: Window, useVirtualKeyboard: Boolean) {
        if (useVirtualKeyboard) {
            evaluate(window)
        } else {
            shouldUpdateNavbarForeground = true
            shouldUpdateNavbarBackground = true
            window.useSystemNavbarBackground(true)
            window.enforceNavbarContrast(false)
        }
        update(window)
    }

    fun update(window: Window) {
        val theme = ThemeManager.activeTheme
        if (shouldUpdateNavbarForeground) {
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = !theme.isDark
        }
        if (shouldUpdateNavbarBackground) {
            window.setNavbarBackgroundColor(
                when (theme) {
                    is Theme.Builtin -> if (keyBorder) theme.backgroundColor else theme.keyboardColor
                    is Theme.Custom -> theme.backgroundColor
                }
            )
        }
    }

    private val ignoreSystemWindowInsets by AppPrefs.getInstance().advanced.ignoreSystemWindowInsets

    private val emptyOnApplyWindowInsetsListener = View.OnApplyWindowInsetsListener { _, insets ->
        insets
    }

    fun setupInputView(v: BaseInputView) {
        if (ignoreSystemWindowInsets) {
            // suppress the view's own onApplyWindowInsets
            v.setOnApplyWindowInsetsListener(emptyOnApplyWindowInsetsListener)
        } else {
            // on API 35+, we must call requestApplyInsets() manually after replacing views,
            // otherwise View#onApplyWindowInsets won't be called. ¯\_(ツ)_/¯
            v.requestApplyInsets()
        }
    }
}
