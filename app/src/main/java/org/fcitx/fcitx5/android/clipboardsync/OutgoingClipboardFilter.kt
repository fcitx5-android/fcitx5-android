package org.fcitx.fcitx5.android.clipboardsync

import android.content.Context
import org.fcitx.fcitx5.android.common.clearurls.ClearUrlsPluginRuntime
import org.fcitx.fcitx5.android.common.clearurls.ClearUrlsRuleFilter
import org.fcitx.fcitx5.android.common.ipc.IFcitxRemoteService

object OutgoingClipboardFilter {
    fun transform(context: Context, remoteService: IFcitxRemoteService?, text: String): String {
        if (!text.startsWith("http://", ignoreCase = true) && !text.startsWith("https://", ignoreCase = true)) {
            return text
        }
        val packageName = resolveLoadedPackage(remoteService) ?: return text
        val filter = ClearUrlsPluginRuntime.loadFilter(context, packageName) ?: return text
        return runCatching { filter.transform(text) }.getOrDefault(text)
    }

    private fun resolveLoadedPackage(remoteService: IFcitxRemoteService?): String? {
        val packages = runCatching { remoteService?.loadedPlugins?.keys }.getOrNull().orEmpty()
        return ClearUrlsPluginRuntime.resolvePackageName(packages)
    }
}
