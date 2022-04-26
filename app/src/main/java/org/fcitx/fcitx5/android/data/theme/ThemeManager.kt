package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceCategory
import org.fcitx.fcitx5.android.utils.appContext
import java.io.File
import java.util.*

object ThemeManager {

    private val dir = File(appContext.getExternalFilesDir(null), "theme").also { it.mkdirs() }

    fun newCustomBackgroundImages(): Triple<String, File, File> {
        val themeName = UUID.randomUUID().toString()
        val croppedImageFile = File(dir, "$themeName-cropped.png")
        val srcImageFile = File(dir, "$themeName-src.png")
        return Triple(themeName, croppedImageFile, srcImageFile)
    }

    private fun themeFile(theme: Theme.Custom) = File(dir, theme.name + ".json")

    fun saveTheme(theme: Theme.Custom) {
        themeFile(theme).writeText(Json.encodeToString(theme))
    }

    fun deleteTheme(theme: Theme.Custom) {
        themeFile(theme).delete()
        theme.backgroundImage?.let {
            File(it.first).delete()
            File(it.second).delete()
        }
    }

    fun listThemes(): List<Theme.Custom> =
        dir.listFiles()?.mapNotNull {
            runCatching { Json.decodeFromString<Theme.Custom>(it.readText()) }.getOrNull()?.apply {
                if (backgroundImage != null) {
                    if (!File(backgroundImage.first).exists())
                        throw IllegalStateException("${backgroundImage.first} specified by $name is missing!")
                    if (!File(backgroundImage.second).exists())
                        throw IllegalStateException("${backgroundImage.second} specified by $name is missing!")
                }
            }
        } ?: listOf()

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

    fun getAllThemes() = listThemes() + builtinThemes

    private val builtinThemes = listOf(
        ThemePreset.PixelLight,
        ThemePreset.PreviewDark
    )

    // TODO
    val currentTheme: Theme = ThemePreset.PixelDark
}