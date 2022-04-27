package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceCategory
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceInternal
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.util.*

object ThemeManager {

    fun interface OnThemeChangedListener {
        fun onThemeChanged(theme: Theme)
    }

    private val dir = File(appContext.getExternalFilesDir(null), "theme").also { it.mkdirs() }

    private val onChangeListeners = WeakHashSet<OnThemeChangedListener>()

    fun addOnChangedListener(listener: OnThemeChangedListener) {
        onChangeListeners.add(listener)
    }

    fun removeOnChangedListener(listener: OnThemeChangedListener) {
        onChangeListeners.remove(listener)
    }

    fun newCustomBackgroundImages(): Triple<String, File, File> {
        val themeName = UUID.randomUUID().toString()
        val croppedImageFile = File(dir, "$themeName-cropped.png")
        val srcImageFile = File(dir, "$themeName-src.png")
        return Triple(themeName, croppedImageFile, srcImageFile)
    }

    private fun getTheme(name: String) =
        customThemes.find { it.name == name } ?: builtinThemes.find { it.name == name }

    private fun themeFile(theme: Theme.Custom) = File(dir, theme.name + ".json")

    fun saveTheme(theme: Theme.Custom) {
        themeFile(theme).writeText(Json.encodeToString(theme))
        customThemes.add(0, theme)
    }

    fun deleteTheme(theme: Theme.Custom) {
        if (theme == currentTheme)
            switchTheme(defaultTheme)
        themeFile(theme).delete()
        theme.backgroundImage?.let {
            File(it.croppedFilePath).delete()
            File(it.srcFilePath).delete()
        }
        customThemes.removeAll { it == theme }
    }

    fun switchTheme(theme: Theme) {
        if (getTheme(theme.name) == null)
            throw IllegalArgumentException("Unknown theme $theme")
        internalPrefs.activeThemeName.setValue(theme.name)
    }

    private fun listThemes(): MutableList<Theme.Custom> =
        dir.listFiles(FileFilter { it.extension == "json" })
            ?.sortedByDescending { it.lastModified() } // newest first
            ?.mapNotNull decode@{
                val theme = runCatching { Json.decodeFromString<Theme.Custom>(it.readText()) }
                    .getOrElse { e ->
                        Timber.w("Failed to decode theme file ${it.absolutePath}: ${e.message}")
                        return@decode null
                    }
                if (theme.backgroundImage != null) {
                    if (!File(theme.backgroundImage.croppedFilePath).exists() ||
                        !File(theme.backgroundImage.srcFilePath).exists()
                    ) {
                        Timber.w("Cannot find background image file for theme ${theme.name}")
                        return@decode null
                    }
                }
                return@decode theme
            }?.toMutableList() ?: mutableListOf()

    class Prefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceCategory(R.string.theme, sharedPreferences) {

        val keyBorder = switch(R.string.key_border, "key_border", false)

        val keyRippleEffect = switch(R.string.key_ripple_effect, "key_ripple_effect", true)

        val keyHorizontalMargin =
            int(R.string.key_horizontal_margin, "key_horizontal_margin", 3, 0, 8)

        val keyVerticalMargin = int(R.string.key_vertical_margin, "key_vertical_margin", 7, 0, 16)

        val keyRadius = int(R.string.key_radius, "key_radius", 4, 0, 48)

    }

    class InternalPrefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceInternal(sharedPreferences) {
        val activeThemeName = string("active_theme_name", defaultTheme.name)
    }

    private val defaultTheme = ThemePreset.PixelDark

    val prefs = AppPrefs.getInstance().registerProvider(false, ::Prefs)

    private val internalPrefs = AppPrefs.getInstance().registerProvider(providerF = ::InternalPrefs)

    private val customThemes = listThemes()

    private val builtinThemes = listOf(
        ThemePreset.MaterialLight,
        ThemePreset.MaterialDark,
        ThemePreset.PixelLight,
        ThemePreset.PixelDark
    )

    private val onActiveThemeNameChange = ManagedPreference.OnChangeListener<String> {
        currentTheme = getTheme(internalPrefs.activeThemeName.getValue())
            ?: throw RuntimeException("Unknown theme $it")
        this@ThemeManager.fireChange()
    }

    private val prefsChange = ManagedPreference.OnChangeListener<Any> {
        this@ThemeManager.fireChange()
    }

    fun init() {
        internalPrefs.activeThemeName.registerOnChangeListener(onActiveThemeNameChange)
        prefs.keyBorder.registerOnChangeListener(prefsChange)
        prefs.keyRadius.registerOnChangeListener(prefsChange)
        prefs.keyRippleEffect.registerOnChangeListener(prefsChange)
        prefs.keyVerticalMargin.registerOnChangeListener(prefsChange)
        prefs.keyHorizontalMargin.registerOnChangeListener(prefsChange)
        // fallback to MaterialLight if active theme was deleted
        internalPrefs.activeThemeName.getValue().let {
            currentTheme = getTheme(it) ?: defaultTheme.apply {
                Timber.w("Cannot find active theme '$it', fallback to $name")
                internalPrefs.activeThemeName.setValue(name)
            }
        }
    }

    private lateinit var currentTheme: Theme

    fun fireChange() {
        onChangeListeners.forEach { it.onThemeChanged(currentTheme) }
    }

    fun getAllThemes() = customThemes + builtinThemes

    fun getActiveTheme() = currentTheme

}