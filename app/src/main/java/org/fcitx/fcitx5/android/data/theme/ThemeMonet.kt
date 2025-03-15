package org.fcitx.fcitx5.android.data.theme

import android.os.Build
import androidx.annotation.RequiresApi
import org.fcitx.fcitx5.android.utils.appContext

// Ref:
// https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md
// https://github.com/material-components/material-components-android/tree/6f41625f5780d8d3e9a0261ee23b84f08b46dcd2/lib/java/com/google/android/material/color/res/color-v31
// https://www.figma.com/community/file/809865700885504168/material-3-android-15

// FIXME: SDK < 34 can only have approximate color values, maybe we can implement our own color algorithm.
// See: https://github.com/XayahSuSuSu/Android-DataBackup/blob/e8b087fb55519c659bebdc46c0217731fe80a0d7/source/core/ui/src/main/kotlin/com/xayah/core/ui/material3/DynamicTonalPalette.kt#L185
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object ThemeMonet {
    val light
        get() = Theme.Monet(
            isDark = false,
            surfaceContainer = appContext.getColor(colorRolesLight.surfaceContainer),
            surfaceBright = appContext.getColor(colorRolesLight.surfaceBright),
            onSurface = appContext.getColor(colorRolesLight.onSurface),
            inversePrimary = appContext.getColor(colorRolesLight.inversePrimary),
            onPrimaryContainer = appContext.getColor(colorRolesLight.onPrimaryContainer),
            secondaryContainer = appContext.getColor(colorRolesLight.secondaryContainer),
            onSecondaryContainer = appContext.getColor(colorRolesLight.onSecondaryContainer)
        )
    val dark
        get() = Theme.Monet(
            isDark = true,
            surfaceContainer = appContext.getColor(colorRolesDark.surfaceContainer),
            surfaceBright = appContext.getColor(colorRolesDark.surfaceBright),
            onSurface = appContext.getColor(colorRolesDark.onSurface),
            inversePrimary = appContext.getColor(colorRolesDark.inversePrimary),
            onPrimaryContainer = appContext.getColor(colorRolesDark.onPrimaryContainer),
            secondaryContainer = appContext.getColor(colorRolesDark.secondaryContainer),
            onSecondaryContainer = appContext.getColor(colorRolesDark.onSecondaryContainer)
        )

    private data object colorRolesLight {
        const val surfaceContainer = android.R.color.system_surface_container_light
        const val surfaceBright = android.R.color.system_surface_bright_light
        const val onSurface = android.R.color.system_on_surface_light
        const val inversePrimary = android.R.color.system_primary_dark
        const val onPrimaryContainer = android.R.color.system_on_primary_container_light
        const val secondaryContainer = android.R.color.system_secondary_container_light
        const val onSecondaryContainer = android.R.color.system_on_secondary_container_light
    }
    private data object colorRolesDark {
        const val surfaceContainer = android.R.color.system_surface_container_dark
        const val surfaceBright = android.R.color.system_surface_bright_dark
        const val onSurface = android.R.color.system_on_surface_dark
        const val inversePrimary = android.R.color.system_primary_light
        const val onPrimaryContainer = android.R.color.system_on_primary_container_dark
        const val secondaryContainer = android.R.color.system_secondary_container_dark
        const val onSecondaryContainer = android.R.color.system_on_secondary_container_dark
    }
}