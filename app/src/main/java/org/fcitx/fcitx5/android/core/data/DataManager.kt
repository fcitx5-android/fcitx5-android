package org.fcitx.fcitx5.android.core.data

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.core.data.DataManager.dataDir
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.FileUtil
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

    const val PLUGIN_INTENT = "${BuildConfig.APPLICATION_ID}.plugin.MANIFEST"

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

    // If Android version supports direct boot, we put the hierarchy in device encrypted storage
    // instead of credential encrypted storage so that data can be accessed before user unlock
    val dataDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Timber.d("Using device protected storage")
        appContext.createDeviceProtectedStorageContext().dataDir
    } else {
        File(appContext.applicationInfo.dataDir)
    }

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

        val pluginPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                Intent(PLUGIN_INTENT),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(PLUGIN_INTENT), PackageManager.MATCH_ALL)
        }.map {
            it.activityInfo.packageName
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
            var hasService = false
            val libraryDependency = mutableMapOf<String, List<String>>()
            var library: String? = null
            var dependency: ArrayList<String>? = null
            var text: String? = null
            while ((eventType != XmlPullParser.END_DOCUMENT)) {
                when (eventType) {
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "library" -> {
                            dependency = arrayListOf()
                            for (i in 0..<parser.attributeCount) {
                                if (parser.getAttributeName(i) == "name") {
                                    library = parser.getAttributeValue(i)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "apiVersion" -> apiVersion = text
                        "domain" -> domain = text
                        "description" -> description = text
                        "hasService" -> hasService = text?.lowercase() == "true"
                        "dependency" -> dependency?.add(text!!)
                        "library" -> {
                            if (library != null && dependency != null) {
                                libraryDependency[library] = dependency
                                library = null
                                dependency = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            parser.close()

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
                            hasService,
                            info.versionName,
                            info.applicationInfo.nativeLibraryDir,
                            libraryDependency
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
                ?: DataDescriptor("", mapOf(), mapOf())

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
                Timber.w("Failed to get or decode data descriptor of '${plugin.name}'")
                Timber.w(it)
            }.getOrNull() ?: continue
            try {
                newHierarchy.install(descriptor, FileSource.Plugin(plugin))
            } catch (e: DataHierarchy.PathConflict) {
                Timber.w("Path '${e.path}' has already been created by '${e.src}', cannot create file")
                failedPlugins[plugin.packageName] =
                    PluginLoadFailed.PathConflict(plugin, e.path, e.src)
                continue
            } catch (e: DataHierarchy.SymlinkConflict) {
                Timber.w("Path '${e.path}' has already been created by '${e.src}', cannot create symlink")
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
                    removePath(it.path).getOrThrow()
                }
                is FileAction.DeleteFile -> {
                    removePath(it.path).getOrThrow()
                }
                is FileAction.UpdateFile -> {
                    val assets = if (it.src is FileSource.Plugin)
                        pluginAssets.getValue(it.src.descriptor.name)
                    else appContext.assets
                    assets.copyFile(it.path)
                }
                is FileAction.CreateSymlink -> {
                    removePath(it.path).getOrThrow()
                    symlink(it.src, it.path).getOrThrow()
                }
            }
        }
        // save the new hierarchy as the data descriptor to be used in the next run
        destDescriptorFile.writeText(serializeDataDescriptor(newHierarchy.downToDataDescriptor()).getOrThrow())
        callbacks.forEach { it() }
        callbacks.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // remove old assets from credential encrypted storage
            val oldDataDir = appContext.dataDir
            val oldDataDescriptor = oldDataDir.resolve(Const.dataDescriptorName)
            if (oldDataDescriptor.exists()) {
                oldDataDescriptor.delete()
                oldDataDir.resolve("README.md").delete()
                oldDataDir.resolve("usr").deleteRecursively()
            }
        }
        synced = true
        Timber.d("Synced")
    }

    private fun removePath(path: String) =
        FileUtil.removeFile(dataDir.resolve(path))

    private fun symlink(source: String, target: String) =
        FileUtil.symlink(dataDir.resolve(source), dataDir.resolve(target))

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
            dataDir.resolve(Const.dataDescriptorName).delete()
            dataDir.resolve("README.md").delete()
            dataDir.resolve("usr").deleteRecursively()
        }
        sync()
    }

}