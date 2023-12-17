package org.fcitx.fcitx5.android.data.theme

import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.errorRuntime
import org.fcitx.fcitx5.android.utils.extract
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

object ThemeFilesManager {

    private val dir = File(appContext.getExternalFilesDir(null), "theme").also { it.mkdirs() }

    private fun themeFile(theme: Theme.Custom) = File(dir, theme.name + ".json")

    fun newCustomBackgroundImages(): Triple<String, File, File> {
        val themeName = UUID.randomUUID().toString()
        val croppedImageFile = File(dir, "$themeName-cropped.png")
        val srcImageFile = File(dir, "$themeName-src")
        return Triple(themeName, croppedImageFile, srcImageFile)
    }

    fun saveThemeFiles(theme: Theme.Custom) {
        themeFile(theme).writeText(Json.encodeToString(CustomThemeSerializer, theme))
    }

    fun deleteThemeFiles(theme: Theme.Custom) {
        themeFile(theme).delete()
        theme.backgroundImage?.let {
            File(it.croppedFilePath).delete()
            File(it.srcFilePath).delete()
        }
    }

    fun listThemes(): MutableList<Theme.Custom> {
        val files = dir.listFiles(FileFilter { it.extension == "json" }) ?: return mutableListOf()
        return files
            .sortedByDescending { it.lastModified() } // newest first
            .mapNotNull decode@{
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
                    saveThemeFiles(theme)
                }
                return@decode theme
            }.toMutableList()
    }

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
                    if (ThemeManager.BuiltinThemes.find { it.name == decoded.name } != null)
                        errorRuntime(R.string.exception_theme_name_clash)
                    val exists = ThemeManager.getTheme(decoded.name) != null
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
                    Triple(!exists, newTheme, migrated)
                }
            }
        }

}
