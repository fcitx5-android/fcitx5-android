package org.fcitx.fcitx5.android.data.clipboard

import org.fcitx.fcitx5.android.common.clearurls.ClearUrlsRuleFilter
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber

object HostClipboardFilter {
    private const val RULES_ASSET = "clearurls/data.min.json"

    private val filter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching {
            appContext.assets.open(RULES_ASSET).bufferedReader().use { reader ->
                ClearUrlsRuleFilter(reader.readText())
            }
        }.onFailure {
            Timber.w(it, "Failed to load bundled ClearURLs rules from %s", RULES_ASSET)
        }.getOrNull()
    }

    fun isEnabled(): Boolean = filter != null

    fun description(): String? =
        filter?.let { "ClearURLs(host)" }

    fun transform(text: String): String {
        val activeFilter = filter ?: return text
        return runCatching { activeFilter.transform(text) }
            .onFailure { Timber.w(it, "Failed to transform clipboard text with host-side ClearURLs") }
            .getOrDefault(text)
    }
}
