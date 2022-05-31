package org.fcitx.fcitx5.android.data.theme

import android.content.SharedPreferences
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
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

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
        val srcImageFile = File(dir, "$themeName-src")
        return Triple(themeName, croppedImageFile, srcImageFile)
    }

    private fun getTheme(name: String) =
        customThemes.find { it.name == name } ?: builtinThemes.find { it.name == name }

    private fun themeFile(theme: Theme.Custom) = File(dir, theme.name + ".json")

    fun saveTheme(theme: Theme.Custom) {
        themeFile(theme).writeText(Json.encodeToString(CustomThemeSerializer, theme))
        val old = customThemes.indexOfFirst { it.name == theme.name }.takeIf { it != -1 }
        old?.let { customThemes[it] = theme } ?: run {
            customThemes.add(0, theme)
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
            ?: throw IllegalArgumentException("Theme $name does not exist")
        themeFile(theme).delete()
        theme.backgroundImage?.let {
            File(it.croppedFilePath).delete()
            File(it.srcFilePath).delete()
        }
        customThemes.remove(theme)
    }

    fun switchTheme(theme: Theme) {
        if (getTheme(theme.name) == null)
            throw IllegalArgumentException("Unregistered theme $theme")
        internalPrefs.activeThemeName.setValue(theme.name)
    }

    private fun listThemes(): MutableList<Theme.Custom> =
        dir.listFiles(FileFilter { it.extension == "json" })
            ?.sortedByDescending { it.lastModified() } // newest first
            ?.mapNotNull decode@{
                val theme =
                    runCatching { Json.decodeFromString(CustomThemeSerializer, it.readText()) }
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


    /**
     * [dest] will be closed on finished
     */
    fun exportTheme(theme: Theme.Custom, dest: OutputStream) =
        runCatching {
            ZipOutputStream(dest.buffered())
                .use { zipStream ->
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
     * @return (newCreated, theme)
     */
    fun importTheme(src: InputStream): Result<Pair<Boolean, Theme.Custom>> = runCatching {
        ZipInputStream(src).use { zipStream ->
            val tempDir = File(createTempDirectory().pathString)
            val extracted = mutableListOf<File>()
            var entry = zipStream.nextEntry
            while (entry != null && !entry.isDirectory) {
                val file = File(tempDir, entry.name)
                zipStream.copyTo(file.outputStream())
                extracted.add(file)
                entry = zipStream.nextEntry
            }
            val jsonFile = extracted.find { it.extension == "json" }
                ?: throw RuntimeException("Unable to find theme json")
            val decoded = Json.decodeFromString(CustomThemeSerializer, jsonFile.readText())
            if (builtinThemes.find { it.name == decoded.name } != null)
                throw RuntimeException("Name clashed with builtin themes")
            val exists = customThemes.find { it.name == decoded.name } != null
            val newTheme = if (decoded.backgroundImage != null) {
                val srcFile = File(dir, decoded.backgroundImage.srcFilePath)
                val croppedFile = File(dir, decoded.backgroundImage.croppedFilePath)
                extracted.find { it.name == srcFile.name }?.copyTo(srcFile)
                    ?: throw RuntimeException("Unable to save src file")
                extracted.find { it.name == croppedFile.name }?.copyTo(croppedFile)
                    ?: throw RuntimeException("Unable to save cropped file")
                decoded.copy(
                    backgroundImage = decoded.backgroundImage.copy(
                        croppedFilePath = srcFile.path,
                        srcFilePath = srcFile.path
                    )
                )
            } else
                decoded
            saveTheme(newTheme)
            !exists to newTheme
        }
    }

    class Prefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceCategory(R.string.theme, sharedPreferences) {

        val keyBorder = switch(R.string.key_border, "key_border", false)

        val keyRippleEffect = switch(R.string.key_ripple_effect, "key_ripple_effect", false)

        val keyHorizontalMargin =
            int(R.string.key_horizontal_margin, "key_horizontal_margin", 3, 0, 8)

        val keyVerticalMargin = int(R.string.key_vertical_margin, "key_vertical_margin", 7, 0, 16)

        val keyRadius = int(R.string.key_radius, "key_radius", 4, 0, 48)

        val punctuationPosition = list(
            R.string.punctuation_position,
            "punctuation_position",
            PunctuationPosition.Bottom,
            PunctuationPosition,
            listOf(
                appContext.getString(R.string.punctuation_pos_bottom) to PunctuationPosition.Bottom,
                appContext.getString(R.string.punctuation_pos_top_right) to PunctuationPosition.TopRight
            )
        )

        enum class PunctuationPosition {
            Bottom,
            TopRight;

            companion object : ManagedPreference.StringLikeCodec<PunctuationPosition> {
                override fun decode(raw: String): PunctuationPosition = valueOf(raw)
            }
        }

        val navbarBackground = list(
            R.string.navbar_background,
            "navbar_background",
            NavbarBackground.Full,
            NavbarBackground,
            listOf(
                appContext.getString(R.string.navbar_bkg_none) to NavbarBackground.None,
                appContext.getString(R.string.navbar_bkg_color_only) to NavbarBackground.ColorOnly,
                appContext.getString(R.string.navbar_bkg_full) to NavbarBackground.Full
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

    }

    class InternalPrefs(sharedPreferences: SharedPreferences) :
        ManagedPreferenceInternal(sharedPreferences) {
        val activeThemeName = string("active_theme_name", defaultTheme.name)
    }

    private val defaultTheme = ThemePreset.PixelDark

    val prefs = AppPrefs.getInstance().registerProvider(false, ::Prefs)

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

    private val onActiveThemeNameChange = ManagedPreference.OnChangeListener<String> {
        currentTheme = getTheme(internalPrefs.activeThemeName.getValue())
            ?: throw RuntimeException("Unknown theme $it")
        this@ThemeManager.fireChange()
    }

    private val prefsChange = ManagedPreference.OnChangeListener<Any> {
        this@ThemeManager.fireChange()
    }

    fun init() {
        prefs.managedPreferences.forEach { (_, pref) ->
            pref.registerOnChangeListener(prefsChange)
        }
        // fallback to default theme if active theme was deleted
        internalPrefs.activeThemeName.getValue().let {
            currentTheme = getTheme(it) ?: defaultTheme.apply {
                Timber.w("Cannot find active theme '$it', fallback to $name")
                internalPrefs.activeThemeName.setValue(name)
            }
        }
        internalPrefs.activeThemeName.registerOnChangeListener(onActiveThemeNameChange)
    }

    private lateinit var currentTheme: Theme

    fun fireChange() {
        onChangeListeners.forEach { it.onThemeChanged(currentTheme) }
    }

    fun getAllThemes() = customThemes + builtinThemes

    fun getActiveTheme() = currentTheme

}