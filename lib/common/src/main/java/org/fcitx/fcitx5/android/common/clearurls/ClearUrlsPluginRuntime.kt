package org.fcitx.fcitx5.android.common.clearurls

import android.content.Context

object ClearUrlsPluginRuntime {
    const val CURRENT_PACKAGE = "org.fcitx.fcitx5.android.plugin.clipboard_filter"
    private const val RULES_ASSET = "data.min.json"

    private val lock = Any()
    private val cachedFilters = mutableMapOf<String, ClearUrlsRuleFilter>()

    fun resolvePackageName(loadedPackages: Set<String>): String? =
        CURRENT_PACKAGE.takeIf { it in loadedPackages }

    fun loadFilter(context: Context, packageName: String, cacheKey: String = packageName): ClearUrlsRuleFilter? {
        cachedFilters[cacheKey]?.let { return it }
        synchronized(lock) {
            cachedFilters[cacheKey]?.let { return it }
            return runCatching {
                val pluginContext = context.createPackageContext(packageName, 0)
                val rawRules = pluginContext.assets.open(RULES_ASSET).bufferedReader().use { it.readText() }
                ClearUrlsRuleFilter(rawRules).also {
                    cachedFilters[cacheKey] = it
                }
            }.getOrNull()
        }
    }
}
