package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceCategory
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceInternal
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.errorArg
import org.fcitx.fcitx5.android.utils.errorRuntime
import org.fcitx.fcitx5.android.utils.errorState
import org.fcitx.fcitx5.android.utils.extract
import org.fcitx.fcitx5.android.utils.isDarkMode
import org.fcitx.fcitx5.android.utils.withTempDir
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ThemeManager {

    fun interface OnThemeChangeListener {
        fun onThemeChange(theme: Theme)
    }

    private val dir = File(appContext.getExternalFilesDir(null), "theme").also { it.mkdirs() }

    private val onChangeListeners = WeakHashSet<OnThemeChangeListener>()

    fun addOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.add(listener)
    }

    fun removeOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.remove(listener)
    }

    fun newCustomBackgroundImages(): Triple<String, File, File> {
        val themeName = UUID.randomUUID().toString()
        val croppedImageFile = File(dir, "$themeName-cropped.png")
        val srcImageFile = File(dir, "$themeName-src")
        return Triple(themeName, croppedImageFile, srcImageFile)
    }

    private fun getTheme(name: String) =
        customThemes.find { it.name == name } ?: builtinThemes.find { it.name == name }

    private fun themeFile(theme: Theme.Custom) = File(dir, theme.name + ".json")

    fun saveTheme(theme: Theme.Custom) {
        themeFile(theme).writeText(Json.encodeToString(CustomThemeSerializer, theme))
        customThemes.indexOfFirst { it.name == theme.name }.let {
            if (it >= 0) customThemes[it] = theme
            else customThemes.add(0, theme)
        }
        if (getActiveTheme().name == theme.name) {
            currentTheme = theme
            fireChange()
        }
    }

    fun deleteTheme(name: String) {
        if (currentTheme.name == name)
            switchTheme(defaultTheme)
        val theme = customThemes.find { it.name == name }
            ?: errorArg(R.string.exception_theme_unknown, name)
        themeFile(theme).delete()
        theme.backgroundImage?.let {
            File(it.croppedFilePath).delete()
            File(it.srcFilePath).delete()
        }
        customThemes.remove(theme)
    }

    fun switchTheme(theme: Theme) {
        if (getTheme(theme.name) == null)
            errorArg(R.string.exception_theme_unknown, theme.name)
        internalPrefs.activeThemeName.setValue(theme.name)
    }

    private fun listThemes(): MutableList<Theme.Custom> =
        dir.listFiles(FileFilter { it.extension == "json" })
            ?.sortedByDescending { it.lastModified() } // newest first
            ?.mapNotNull decode@{
                val (theme, migrated) = runCatching {
                    Json.decodeFromString(CustomThemeSerializer.WithMigrationStatus, it.readText())
                }.getOrElse { e ->
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
                // Update the saved file if migration happens
                if (migrated) {
                    // We can't use saveTheme here, since customThemes might have not been initialized
                    themeFile(theme).writeText(Json.encodeToString(CustomThemeSerializer, theme))
                }
                return@decode theme
            }?.toMutableList() ?: mutableListOf()


    /**
     * [dest] will be closed on finished
     */
    fun exportTheme(theme: Theme.Custom, dest: OutputStream) =
        runCatching {
            ZipOutputStream(dest.buffered()).use { zipStream ->
                // we don't export the internal path of images
                val tweakedTheme = theme.backgroundImage?.let {
                    theme.copy(
                        backgroundImage = theme.backgroundImage.copy(
                            croppedFilePath = theme.backgroundImage.croppedFilePath
                                .substringAfterLast('/'),
                            srcFilePath = theme.backgroundImage.srcFilePath
                                .substringAfterLast('/'),
                        )
                    )
                } ?: theme
                if (tweakedTheme.backgroundImage != null) {
                    requireNotNull(theme.backgroundImage)
                    // write cropped image
                    zipStream.putNextEntry(ZipEntry(tweakedTheme.backgroundImage.croppedFilePath))
                    File(theme.backgroundImage.croppedFilePath).inputStream()
                        .use { it.copyTo(zipStream) }
                    // write src image
                    zipStream.putNextEntry(ZipEntry(tweakedTheme.backgroundImage.srcFilePath))
                    File(theme.backgroundImage.srcFilePath).inputStream()
                        .use { it.copyTo(zipStream) }
                }
                // write json
                zipStream.putNextEntry(ZipEntry("${tweakedTheme.name}.json"))
                zipStream.write(
                    Json.encodeToString(CustomThemeSerializer, tweakedTheme)
                        .encodeToByteArray()
                )
                // done
                zipStream.closeEntry()
            }
        }

    /**
     * @return (newCreated, theme, migrated)
     */
    fun importTheme(src: InputStream): Result<Triple<Boolean, Theme.Custom, Boolean>> =
        runCatching {
            ZipInputStream(src).use { zipStream ->
                withTempDir { tempDir ->
                    val extracted = zipStream.extract(tempDir)
                    val jsonFile = extracted.find { it.extension == "json" }
                        ?: errorRuntime(R.string.exception_theme_json)
                    val (decoded, migrated) = Json.decodeFromString(
                        CustomThemeSerializer.WithMigrationStatus,
                        jsonFile.readText()
                    )
                    if (builtinThemes.find { it.name == decoded.name } != null)
                        errorRuntime(R.string.exception_theme_name_clash)
                    val exists = customThemes.find { it.name == decoded.name } != null
                    val newTheme = if (decoded.backgroundImage != null) {
                        val srcFile = File(dir, decoded.backgroundImage.srcFilePath)
                        val croppedFile = File(dir, decoded.backgroundImage.croppedFilePath)
                        extracted.find { it.name == srcFile.name }?.copyTo(srcFile)
                            ?: errorRuntime(R.string.exception_theme_src_image)
                        extracted.find { it.name == croppedFile.name }?.copyTo(croppedFile)
                            ?: errorRuntime(R.string.exception_theme_cropped_image)
                        decoded.copy(
                            backgroundImage = decoded.backgroundImage.copy(
                                croppedFilePath = croppedFile.path,
                                srcFilePath = srcFile.path
                            )
                        )
                    } else {
                        decoded
                    }
                    saveTheme(newTheme)
                    Triple(!exists, newTheme, migrated)
                }
            }
        }

    class Prefs(sharedPreferences: SharedPreferences) :
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
            NavbarBackground.Full,
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

    class InternalPrefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceInternal(sharedPreferences) {
        val activeThemeName = string("active_theme_name", defaultTheme.name)
    }

    private val defaultTheme = ThemePreset.PixelDark

    val prefs = AppPrefs.getInstance().registerProvider(::Prefs)

    private val internalPrefs = AppPrefs.getInstance().registerProvider(providerF = ::InternalPrefs)

    private val customThemes = listThemes()

    val builtinThemes = listOf(
        ThemePreset.MaterialLight,
        ThemePreset.MaterialDark,
        ThemePreset.PixelLight,
        ThemePreset.PixelDark,
        ThemePreset.NordLight,
        ThemePreset.NordDark,
        ThemePreset.DeepBlue,
        ThemePreset.Monokai,
        ThemePreset.AMOLEDBlack,
    )

    @Keep
    private val onActiveThemeNameChange = ManagedPreference.OnChangeListener<String> { _, it ->
        currentTheme = getTheme(internalPrefs.activeThemeName.getValue())
            ?: errorState(R.string.exception_theme_unknown, it)
        fireChange()
    }

    @Keep
    private val onThemePrefsChange = ManagedPreference.OnChangeListener<Any> { key, _ ->
        fireChange()
        if (prefs.dayNightModePrefNames.contains(key)) {
            onSystemDarkModeChange()
        }
    }

    fun init(configuration: Configuration) {
        isCurrentDark = configuration.isDarkMode()
        // fire all `OnThemeChangedListener`s on theme preferences change
        prefs.managedPreferences.values.forEach {
            it.registerOnChangeListener(onThemePrefsChange)
        }
        currentTheme = if (prefs.followSystemDayNightTheme.getValue()) {
            (if (isCurrentDark) prefs.darkModeTheme else prefs.lightModeTheme).getValue()
        } else {
            val activeThemeName = internalPrefs.activeThemeName.getValue()
            // fallback to default theme if active theme not found
            getTheme(activeThemeName) ?: defaultTheme.also {
                Timber.w("Cannot find active theme '$activeThemeName', fallback to ${it.name}")
                internalPrefs.activeThemeName.setValue(it.name)
            }
        }
        internalPrefs.activeThemeName.registerOnChangeListener(onActiveThemeNameChange)
    }

    private lateinit var currentTheme: Theme

    private fun fireChange() {
        onChangeListeners.forEach { it.onThemeChange(currentTheme) }
    }

    fun getAllThemes() = customThemes + builtinThemes

    fun getActiveTheme() = currentTheme

    private var isCurrentDark = false

    fun onSystemDarkModeChange(isDark: Boolean = isCurrentDark) {
        isCurrentDark = isDark
        if (prefs.followSystemDayNightTheme.getValue()) {
            switchTheme((if (isDark) prefs.darkModeTheme else prefs.lightModeTheme).getValue())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun syncToDeviceEncryptedStorage() {
        val ctx = appContext.createDeviceProtectedStorageContext()
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        sp.edit {
            internalPrefs.managedPreferences.forEach {
                it.value.putValueTo(this@edit)
            }
            prefs.managedPreferences.forEach {
                it.value.putValueTo(this@edit)
            }
        }
    }

}