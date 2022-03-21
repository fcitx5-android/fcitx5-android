package org.fcitx.fcitx5.android.utils

import android.content.Context
import android.content.res.Configuration
import org.fcitx.fcitx5.android.BuildConfig

// Adapted from https://gist.github.com/hendrawd/01f215fd332d84793e600e7f82fc154b
object DeviceInfo {
    fun get(context: Context) =
        buildString {
            appendLine("App Package Name: ${BuildConfig.APPLICATION_ID}")
            appendLine("App Version Name: ${BuildConfig.VERSION_NAME}")
            appendLine("App Version Code: ${BuildConfig.VERSION_CODE}")
            appendLine("OS Name: ${android.os.Build.DISPLAY}")
            appendLine("OS Version: ${System.getProperty("os.version")} (${android.os.Build.VERSION.INCREMENTAL})")
            appendLine("OS API Level: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Device: ${android.os.Build.DEVICE}")
            appendLine("Model (product): ${android.os.Build.MODEL} (${android.os.Build.PRODUCT})")
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Tags: ${android.os.Build.TAGS}")
            val metrics = context.resources.displayMetrics
            appendLine("Screen Size: ${metrics.widthPixels} x ${metrics.heightPixels}")
            appendLine("Screen Density: ${metrics.density}")
            appendLine(
                "Screen orientation: ${
                    when (context.resources.configuration.orientation) {
                        Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                        Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                        Configuration.ORIENTATION_UNDEFINED -> "Undefined"
                        else -> "Unknown"
                    }
                }"
            )
        }.lines()

}