package org.fcitx.fcitx5.android.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.errorRuntime
import org.fcitx.fcitx5.android.utils.extract
import org.fcitx.fcitx5.android.utils.withTempDir
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object UserDataManager {

    private val json = Json { prettyPrint = true }

    @Serializable
    data class Metadata(
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
        val exportTime: Long
    )

    private fun writeFileTree(srcDir: File, destPrefix: String, dest: ZipOutputStream) {
        dest.putNextEntry(ZipEntry("$destPrefix/"))
        srcDir.walkTopDown().forEach { f ->
            val related = f.relativeTo(srcDir)
            if (related.path != "") {
                val entry =
                    ZipEntry("$destPrefix/${related.path}${if (f.isDirectory) "/" else ""}")
                dest.putNextEntry(entry)
                if (f.isFile)
                    f.inputStream().use { it.copyTo(dest) }
            }
        }
    }

    private val sharedPrefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
    private val dataBasesDir = File(appContext.applicationInfo.dataDir, "databases")
    private val externalDir = appContext.getExternalFilesDir(null)!!
    private val recentlyUsedDir = appContext.filesDir.resolve(RecentlyUsed.DIR_NAME)

    @OptIn(ExperimentalSerializationApi::class)
    fun export(dest: OutputStream) = runCatching {
        ZipOutputStream(dest.buffered()).use { zipStream ->
            // shared_prefs
            writeFileTree(sharedPrefsDir, "shared_prefs", zipStream)
            // databases
            writeFileTree(dataBasesDir, "databases", zipStream)
            // external
            writeFileTree(externalDir, "external", zipStream)
            // recently_used
            writeFileTree(recentlyUsedDir, "recently_used", zipStream)
            // metadata
            zipStream.putNextEntry(ZipEntry("metadata.json"))
            val metadata = Metadata(
                BuildConfig.APPLICATION_ID,
                BuildConfig.VERSION_CODE,
                Const.versionName,
                System.currentTimeMillis()
            )
            json.encodeToStream(metadata, zipStream)
            zipStream.closeEntry()
        }
    }

    fun import(src: InputStream) = runCatching {
        ZipInputStream(src).use { zipStream ->
            withTempDir { tempDir ->
                val extracted = zipStream.extract(tempDir)
                val metadataFile = extracted.find { it.name == "metadata.json" }
                    ?: errorRuntime(R.string.exception_user_data_metadata)
                val metadata = json.decodeFromString<Metadata>(metadataFile.readText())
                if (metadata.packageName != BuildConfig.APPLICATION_ID)
                    errorRuntime(R.string.exception_user_data_package_name_mismatch)
                // shared_prefs
                File(tempDir, "shared_prefs").copyRecursively(target = sharedPrefsDir, overwrite = true)
                // databases
                File(tempDir, "databases").copyRecursively(target = dataBasesDir, overwrite = true)
                // external
                File(tempDir, "external").copyRecursively(target = externalDir, overwrite = true)
                // recently_used
                File(tempDir, "recently_used").copyRecursively(target = recentlyUsedDir, overwrite = true)
                metadata
            }
        }
    }
}