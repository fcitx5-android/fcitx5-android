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
import java.io.File
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

    private fun themeFile(theme: Theme.Custom) = File(dir, theme.name + ".json")

    fun saveTheme(theme: Theme.Custom) {
        themeFile(theme).writeText(Json.encodeToString(theme))
        knownThemes[theme.name] = theme
    }

    fun deleteTheme(theme: Theme.Custom) {
        if (theme == currentTheme)
            switchTheme(defaultTheme)
        themeFile(theme).delete()
        theme.backgroundImage?.let {
            File(it.croppedFilePath).delete()
            File(it.srcFilePath).delete()
        }
        knownThemes.remove(theme.name)
    }

    fun switchTheme(theme: Theme) {
        if (theme.name !in knownThemes)
            throw IllegalArgumentException("Unknown theme $theme")
        internalPrefs.activeThemeName.setValue(theme.name)
    }

    private fun listThemes(): List<Theme.Custom> =
        dir.listFiles()?.mapNotNull {
            runCatching { Json.decodeFromString<Theme.Custom>(it.readText()) }.getOrNull()?.apply {
                if (backgroundImage != null) {
                    if (!File(backgroundImage.croppedFilePath).exists())
                        throw IllegalStateException("${backgroundImage.croppedFilePath} specified by $name is missing!")
                    if (!File(backgroundImage.srcFilePath).exists())
                        throw IllegalStateException("${backgroundImage.srcFilePath} specified by $name is missing!")
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

    class InternalPrefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceInternal(sharedPreferences) {
        val activeThemeName = string("active_theme_name", defaultTheme.name)
    }

    private val defaultTheme = ThemePreset.PixelDark

    val prefs = AppPrefs.getInstance().registerProvider(false, ::Prefs)

    private val internalPrefs = AppPrefs.getInstance().registerProvider(providerF = ::InternalPrefs)

    private val knownThemes = mutableMapOf<String, Theme>()

    private val builtinThemes = listOf(
        ThemePreset.MaterialLight,
        ThemePreset.MaterialDark,
        ThemePreset.PixelLight,
        ThemePreset.PixelDark
    )

    private val onActiveThemeNameChange = ManagedPreference.OnChangeListener<String> {
        currentTheme = internalPrefs.activeThemeName.getValue()
            .let { knownThemes[it] ?: throw RuntimeException("Unknown theme $it") }
        this@ThemeManager.fireChange()
    }

    private val prefsChange = ManagedPreference.OnChangeListener<Any> {
        this@ThemeManager.fireChange()
    }

    fun init() {
        listThemes().forEach {
            knownThemes[it.name] = it
        }
        builtinThemes.forEach {
            knownThemes[it.name] = it
        }
        internalPrefs.activeThemeName.registerOnChangeListener(onActiveThemeNameChange)
        prefs.keyBorder.registerOnChangeListener(prefsChange)
        prefs.keyRadius.registerOnChangeListener(prefsChange)
        prefs.keyRippleEffect.registerOnChangeListener(prefsChange)
        prefs.keyVerticalMargin.registerOnChangeListener(prefsChange)
        prefs.keyHorizontalMargin.registerOnChangeListener(prefsChange)
        currentTheme = internalPrefs.activeThemeName.getValue()
            .let { knownThemes[it] ?: throw RuntimeException("Unknown theme $it") }
    }

    private lateinit var currentTheme: Theme

    fun fireChange() {
        onChangeListeners.forEach { it.onThemeChanged(currentTheme) }
    }

    fun getAllThemes() = knownThemes.values.sortedBy { it is Theme.Builtin }

    fun getActiveTheme() = currentTheme

}