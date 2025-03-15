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
                surfaceBright = appContext.getColor(android.R.color.system_surface_bright_light),
                onSurface = appContext.getColor(android.R.color.system_on_surface_light),
                inversePrimary = appContext.getColor(android.R.color.system_accent1_200),
                onPrimaryContainer = appContext.getColor(android.R.color.system_on_primary_container_light),
                secondaryContainer = appContext.getColor(android.R.color.system_secondary_container_light),
                onSecondaryContainer = appContext.getColor(android.R.color.system_on_secondary_container_light)
            )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Approximate color values
            Theme.Monet(
                isDark = false,
                surfaceContainer = appContext.getColor(android.R.color.system_neutral1_50), // N94
                surfaceBright = appContext.getColor(android.R.color.system_neutral1_10), // N98
                onSurface = appContext.getColor(android.R.color.system_neutral1_900),
                inversePrimary = appContext.getColor(android.R.color.system_accent1_200),
                onPrimaryContainer = appContext.getColor(android.R.color.system_accent1_700),
                secondaryContainer = appContext.getColor(android.R.color.system_accent2_100),
                onSecondaryContainer = appContext.getColor(android.R.color.system_accent2_700)
            )
        else // Static MD3 colors, based on #769CDF
            Theme.Monet(
                isDark = false,
                surfaceContainer = 0xffededf4.toInt(),
                surfaceBright = 0xfff9f9ff.toInt(),
                onSurface = 0xff191c20.toInt(),
                inversePrimary = 0xffaac7ff.toInt(),
                onPrimaryContainer = 0xff284777.toInt(),
                secondaryContainer = 0xffdae2f9.toInt(),
                onSecondaryContainer = 0xff3e4759.toInt(),
            )
    val dark
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) // Real Monet colors
            Theme.Monet(
                isDark = true,
                surfaceContainer = appContext.getColor(android.R.color.system_surface_container_dark),
                surfaceBright = appContext.getColor(android.R.color.system_surface_bright_dark),
                onSurface = appContext.getColor(android.R.color.system_on_surface_dark),
                inversePrimary = appContext.getColor(android.R.color.system_accent1_600),
                onPrimaryContainer = appContext.getColor(android.R.color.system_on_primary_container_dark),
                secondaryContainer = appContext.getColor(android.R.color.system_secondary_container_dark),
                onSecondaryContainer = appContext.getColor(android.R.color.system_on_secondary_container_dark)
            )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Approximate color values
            Theme.Monet(
                isDark = true,
                surfaceContainer = appContext.getColor(android.R.color.system_neutral1_900), // N12
                surfaceBright = appContext.getColor(android.R.color.system_neutral1_800), // N24
                onSurface = appContext.getColor(android.R.color.system_neutral1_100),
                inversePrimary = appContext.getColor(android.R.color.system_accent1_600),
                onPrimaryContainer = appContext.getColor(android.R.color.system_accent1_100),
                secondaryContainer = appContext.getColor(android.R.color.system_accent2_700),
                onSecondaryContainer = appContext.getColor(android.R.color.system_accent2_100)
            )
        else // Static MD3 colors, based on #769CDF
            Theme.Monet(
                isDark = true,
                surfaceContainer = 0xff1d2024.toInt(),
                surfaceBright = 0xff37393e.toInt(),
                onSurface = 0xffe2e2e9.toInt(),
                inversePrimary = 0xff415f91.toInt(),
                onPrimaryContainer = 0xffd6e3ff.toInt(),
                secondaryContainer = 0xff3e4759.toInt(),
                onSecondaryContainer = 0xffdae2f9.toInt(),
            )
}