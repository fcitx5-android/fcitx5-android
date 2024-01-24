/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.core.view.WindowCompat

fun Activity.applyTranslucentSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    // View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR is available on 23+ (and our minSDK is 23)
    window.statusBarColor = Color.TRANSPARENT
    // View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR is available on 26+
    window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Color.TRANSPARENT
    } else {
        // com.android.internal.R.color.system_bar_background_semi_transparent
        0x66000000
    }
    val lightSystemBars = !resources.configuration.isDarkMode()
    WindowCompat.getInsetsController(window, window.decorView).apply {
        // only works on 23+
        isAppearanceLightStatusBars = lightSystemBars
        // only works on 26+
        isAppearanceLightNavigationBars = lightSystemBars
    }
}
