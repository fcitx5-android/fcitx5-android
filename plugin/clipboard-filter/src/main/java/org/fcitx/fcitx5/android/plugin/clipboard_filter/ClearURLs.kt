/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.clipboard_filter

import android.content.res.AssetManager
import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.URLDecoder


object ClearURLs {
    @Serializable
    data class ClearURLsProvider(
        val urlPattern: String,
        val completeProvider: Boolean = false,
        val rules: List<String>? = null,
        val rawRules: List<String>? = null,
        val referralMarketing: List<String>? = null,
        val exceptions: List<String>? = null,
        val redirections: List<String>? = null,
        val forceRedirection: Boolean = false
    )

    private val providersSerializer: KSerializer<Map<String, ClearURLsProvider>> = serializer()
    private val catalogSerializer: KSerializer<Map<String, Map<String, ClearURLsProvider>>> =
        MapSerializer(serializer(), providersSerializer)

    private var catalog: Map<String, ClearURLsProvider>? = null

    fun initCatalog(assetManager: AssetManager) {
        if (catalog != null)
            return
        catalog = Json.decodeFromString(
            catalogSerializer,
            assetManager.open("data.minify.json").bufferedReader().readText()
        )["providers"]
    }

    fun transform(text: String): String {
        var x = text
        catalog?.let { map ->
            for ((_, provider) in map) {
                // matches url pattern
                if (provider.urlPattern.toRegex().find(x) == null)
                    continue
                // not in exceptions
                if (provider.exceptions?.any { it.toRegex().find(x) != null } == true)
                    continue
                // apply redirections
                provider.redirections?.forEach { redirection ->
                    redirection.toRegex().find(x)?.let { matchResult ->
                        matchResult.groupValues.takeIf { it.size > 1 }?.let {
                            val redir = decodeURL(it[1])
                            x = redir
                        }
                    }
                }
                provider.rawRules?.forEach { rawRule ->
                    val r = rawRule.toRegex()
                    x = r.replace(x, "")
                }
                // apply rules
                provider.rules?.forEach { rule ->
                    val r = "(?:&amp;|[/?#&])$rule=[^&]*".toRegex()
                    x = r.replace(x, "")
                }
            }
            Log.d("ClearURLs", "$text -> $x")
            return x
        } ?: throw IllegalStateException("Catalog is unavailable")
    }

    private fun decodeURL(url: String) =
        URLDecoder.decode(url.replace("+", "%2B"), "UTF-8").replace("%2B", "+")
}
