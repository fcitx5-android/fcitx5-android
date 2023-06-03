package org.fcitx.fcitx5.android.core.data

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.core.data.DataManager.dataDir
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.javaIdRegex
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Build up a Filesystem hierarchy at [dataDir]
 *
 * Operations are synchronized
 */
object DataManager {

    const val PLUGIN_INTENT = "org.fcitx.fcitx5.android.plugin.MANIFEST"

    private val lock = ReentrantLock()

    private val json by lazy { Json { prettyPrint = true } }

    var synced = false
        private set

    // should be consistent with the deserialization in build.gradle.kts (:app)
    private fun deserializeDataDescriptor(raw: String) = runCatching {
        json.decodeFromString<DataDescriptor>(raw)
    }

    private fun serializeDataDescriptor(descriptor: DataDescriptor) = runCatching {
        json.encodeToString(descriptor)
    }

    val dataDir = File(appContext.applicationInfo.dataDir)
    private val destDescriptorFile = File(dataDir, Const.dataDescriptorName)

    private fun AssetManager.getDataDescriptor() =
        open(Const.dataDescriptorName)
            .bufferedReader()
            .use { it.readText() }
            .let { deserializeDataDescriptor(it) }
            .getOrThrow()

    private val loadedPlugins = mutableSetOf<PluginDescriptor>()
    private val failedPlugins = mutableMapOf<String, PluginLoadFailed>()

    fun getLoadedPlugins(): Set<PluginDescriptor> = loadedPlugins
    fun getFailedPlugins(): Map<String, PluginLoadFailed> = failedPlugins

    /**
     * Will be cleared after each sync
     */
    private val callbacks = mutableListOf<() -> Unit>()

    fun addOnNextSyncedCallback(block: () -> Unit) =
        callbacks.add(block)

    @SuppressLint("DiscouragedApi")
    fun detectPlugins(): Pair<Set<PluginDescriptor>, Map<String, PluginLoadFailed>> {
        val toLoad = mutableSetOf<PluginDescriptor>()
        val preloadFailed = mutableMapOf<String, PluginLoadFailed>()

        val pm = appContext.packageManager

        val isDebugBuild = Const.buildType == "debug"
        val pluginPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                Intent(PLUGIN_INTENT),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(PLUGIN_INTENT), PackageManager.MATCH_ALL)
        }.mapNotNull {
            // Only consider plugin with the same build variant as app's
            val packageName = it.activityInfo.packageName
            if (isDebugBuild == packageName.endsWith(".debug")) packageName else null
        }

        Timber.d("Detected plugin packages: ${pluginPackages.joinToString()}")

        // Parse plugin.xml
        for (packageName in pluginPackages) {
            val res = pm.getResourcesForApplication(packageName)
            val resId = res.getIdentifier("plugin", "xml", packageName)
            if (resId == 0) {
                Timber.w("Failed to get the plugin descriptor of $packageName")
                failedPlugins[packageName] = PluginLoadFailed.MissingPluginDescriptor
                continue
            }
            val parser = res.getXml(resId)
            var eventType = parser.eventType
            var domain: String? = null
            var apiVersion: String? = null
            var description: String? = null
            var text: String? = null
            while ((eventType != XmlPullParser.END_DOCUMENT)) {
                when (eventType) {
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "apiVersion" -> apiVersion = text
                        "domain" -> domain = text
                        "description" -> description = text
                    }
                }
                eventType = parser.next()
            }

            // Replace @string/ with string resource
            description = description?.let { d ->
                d.removePrefix("@string/").let { s ->
                    if (s.matches(javaIdRegex)) {
                        res.getIdentifier(s, "string", packageName).let { id ->
                            if (id != 0) res.getString(id) else d
                        }
                    } else d
                }
            }

            if (apiVersion != null && description != null) {
                if (PluginDescriptor.pluginAPI == apiVersion) {
                    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(
                            packageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                    }
                    toLoad.add(
                        PluginDescriptor(
                            packageName,
                            apiVersion,
                            domain,
                            description,
                            info.versionName,
                            info.applicationInfo.nativeLibraryDir
                        )
                    )
                } else {
                    Timber.w("$packageName's api version [$apiVersion] doesn't match with the current [${PluginDescriptor.pluginAPI}]")
                    preloadFailed[packageName] = PluginLoadFailed.PluginAPIIncompatible(apiVersion)
                }
            } else {
                Timber.w("Failed to parse plugin descriptor of $packageName")
                preloadFailed[packageName] = PluginLoadFailed.PluginDescriptorParseError
            }
        }
        return toLoad to preloadFailed
    }

    fun sync() = lock.withLock {
        synced = false
        loadedPlugins.clear()
        failedPlugins.clear()

        // load last run's data descriptor
        val oldDescriptor =
            destDescriptorFile
                .takeIf { it.exists() && it.isFile }
                ?.runCatching { readText() }
                ?.getOrNull()
                ?.let { deserializeDataDescriptor(it) }
                ?.getOrNull()
                ?: DataDescriptor("", mapOf())

        // load app's data descriptor
        val mainDescriptor =
            appContext.assets
                .open(Const.dataDescriptorName)
                .bufferedReader()
                .use { it.readText() }
                .let { deserializeDataDescriptor(it) }
                .getOrThrow()

        val (parsedDescriptors, failed) = detectPlugins()
        failedPlugins.putAll(failed)

        Timber.d("Plugins to load: $parsedDescriptors")

        // Create an empty hierarchy
        val newHierarchy = DataHierarchy()
        // Always add app's first
        newHierarchy.install(mainDescriptor, FileSource.Main)

        val pluginAssets = mutableMapOf<String, AssetManager>()

        // Add plugin's one by one
        for (plugin in parsedDescriptors) {
            val pluginContext = appContext.createPackageContext(plugin.packageName, 0)
            val assets = pluginContext.assets
            val descriptor = runCatching { assets.getDataDescriptor() }.onFailure {
                it.printStackTrace()
                Timber.w("Failed to get and decode the data descriptor of ${plugin.name}")
            }.getOrNull() ?: continue
            try {
                newHierarchy.install(descriptor, FileSource.Plugin(plugin))
            } catch (e: DataHierarchy.Conflict) {
                Timber.w("Path ${e.path} is already created by ${e.src}")
                failedPlugins[plugin.packageName] =
                    PluginLoadFailed.PathConflict(plugin, e.path, e.src)
                continue
            }
            pluginAssets[plugin.name] = assets
            loadedPlugins.add(plugin)
            Timber.d("Merged data hierarchy of ${plugin.name}")
        }

        Timber.d("Hierarchy created")

        // Compute the difference of the created one and the old one
        // Run actions to migrate to the new hierarchy
        DataHierarchy.diff(oldDescriptor, newHierarchy).sortedByDescending { it.ordinal }.forEach {
            Timber.d("Action: $it")
            when (it) {
                is FileAction.CreateFile -> {
                    val assets = if (it.src is FileSource.Plugin)
                        pluginAssets.getValue(it.src.descriptor.name)
                    else appContext.assets
                    assets.copyFile(it.path)
                }
                is FileAction.DeleteDir -> {
                    deleteDir(it.path)
                }
                is FileAction.DeleteFile -> {
                    deleteFile(it.path)
                }
                is FileAction.UpdateFile -> {
                    val assets = if (it.src is FileSource.Plugin)
                        pluginAssets.getValue(it.src.descriptor.name)
                    else appContext.assets
                    assets.copyFile(it.path)
                }
            }
        }
        // save the new hierarchy as the data descriptor to be used in the next run
        destDescriptorFile.writeText(serializeDataDescriptor(newHierarchy.downToDataDescriptor()).getOrThrow())
        callbacks.forEach { it() }
        callbacks.clear()
        synced = true
        Timber.d("Synced")
    }

    private fun deleteFile(path: String) {
        val file = File(dataDir, path)
        if (file.exists() && file.isFile)
            file.delete()
    }

    private fun deleteDir(path: String) {
        val dir = File(dataDir, path)
        if (dir.exists() && dir.isDirectory)
            dir.deleteRecursively()
    }

    private fun AssetManager.copyFile(filename: String) {
        open(filename).use { i ->
            File(dataDir, filename)
                .also { it.parentFile?.mkdirs() }
                .outputStream()
                .use { o -> i.copyTo(o) }
        }
    }

    fun deleteAndSync() {
        lock.withLock {
            dataDir.deleteRecursively()
        }
        sync()
    }

}