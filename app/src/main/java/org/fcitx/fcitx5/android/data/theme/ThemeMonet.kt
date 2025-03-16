/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import android.os.Build
import org.fcitx.fcitx5.android.utils.appContext

// Ref:
// https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md
// https://www.figma.com/community/file/809865700885504168/material-3-android-15
// https://material-foundation.github.io/material-theme-builder/

// FIXME: SDK < 34 can only have approximate color values, maybe we can implement our own color algorithm.
// See: https://github.com/XayahSuSuSu/Android-DataBackup/blob/e8b087fb55519c659bebdc46c0217731fe80a0d7/source/core/ui/src/main/kotlin/com/xayah/core/ui/material3/DynamicTonalPalette.kt#L185

object ThemeMonet {
    val light
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) // Real Monet colors
            Theme.Monet(
                isDark = false,
                surfaceContainer = appContext.getColor(android.R.color.system_surface_container_light),
                surfaceContainerHigh = appContext.getColor(android.R.color.system_surface_container_highest_light),
                surfaceBright = appContext.getColor(android.R.color.system_surface_bright_light),
                onSurface = appContext.getColor(android.R.color.system_on_surface_light),
                primary = appContext.getColor(android.R.color.system_primary_light),
                onPrimary = appContext.getColor(android.R.color.system_on_primary_light),
                secondaryContainer = appContext.getColor(android.R.color.system_secondary_container_light),
                onSurfaceVariant = appContext.getColor(android.R.color.system_on_surface_variant_light)
            )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Approximate color values
            Theme.Monet(
                isDark = false,
                surfaceContainer = appContext.getColor(android.R.color.system_neutral1_50), // neutral94
                surfaceContainerHigh = appContext.getColor(android.R.color.system_neutral2_100), // neutral92
                surfaceBright = appContext.getColor(android.R.color.system_neutral1_10), // neutral98
                onSurface = appContext.getColor(android.R.color.system_neutral1_900),
                primary = appContext.getColor(android.R.color.system_accent1_600),
                onPrimary = appContext.getColor(android.R.color.system_accent1_0),
                secondaryContainer = appContext.getColor(android.R.color.system_accent2_100),
                onSurfaceVariant = appContext.getColor(android.R.color.system_accent2_700)
            )
        else // Static MD3 colors, based on #769CDF
            Theme.Monet(
                isDark = false,
                surfaceContainer = 0xffededf4.toInt(),
                surfaceContainerHigh = 0xffe7e8ee.toInt(),
                surfaceBright = 0xfff9f9ff.toInt(),
                onSurface = 0xff191c20.toInt(),
                primary = 0xff415f91.toInt(),
                onPrimary = 0xffffffff.toInt(),
                secondaryContainer = 0xffdae2f9.toInt(),
                onSurfaceVariant = 0xff44474e.toInt(),
            )
    val dark
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) // Real Monet colors
            Theme.Monet(
                isDark = true,
                surfaceContainer = appContext.getColor(android.R.color.system_surface_container_dark),
                surfaceContainerHigh = appContext.getColor(android.R.color.system_surface_container_high_dark),
                surfaceBright = appContext.getColor(android.R.color.system_surface_bright_dark),
                onSurface = appContext.getColor(android.R.color.system_on_surface_dark),
                primary = appContext.getColor(android.R.color.system_primary_dark),
                onPrimary = appContext.getColor(android.R.color.system_on_primary_dark),
                secondaryContainer = appContext.getColor(android.R.color.system_secondary_container_dark),
                onSurfaceVariant = appContext.getColor(android.R.color.system_on_surface_variant_dark)
            )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Approximate color values
            Theme.Monet(
                isDark = true,
                surfaceContainer = appContext.getColor(android.R.color.system_neutral1_900), // neutral12
                surfaceContainerHigh = appContext.getColor(android.R.color.system_neutral2_1000), // neutral17
                surfaceBright = appContext.getColor(android.R.color.system_neutral1_800), // neutral24
                onSurface = appContext.getColor(android.R.color.system_neutral1_100),
                primary = appContext.getColor(android.R.color.system_accent1_200),
                onPrimary = appContext.getColor(android.R.color.system_accent1_800),
                secondaryContainer = appContext.getColor(android.R.color.system_accent2_700),
                onSurfaceVariant = appContext.getColor(android.R.color.system_accent2_200)
            )
        else // Static MD3 colors, based on #769CDF
            Theme.Monet(
                isDark = true,
                surfaceContainer = 0xff1d2024.toInt(),
                surfaceContainerHigh = 0xff282a2f.toInt(),
                surfaceBright = 0xff37393e.toInt(),
                onSurface = 0xffe2e2e9.toInt(),
                primary = 0xffaac7ff.toInt(),
                onPrimary = 0xff0a305f.toInt(),
                secondaryContainer = 0xff3e4759.toInt(),
                onSurfaceVariant = 0xffc4c6d0.toInt(),
            )
}