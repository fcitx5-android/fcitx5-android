/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.clipboard_filter

import android.content.res.AssetManager
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.URLDecoder

typealias RegexAsString = @Serializable(with = RegexSerializer::class) Regex
typealias SearchParam = UrlQuerySanitizer.ParameterValuePair
typealias SearchParams = List<UrlQuerySanitizer.ParameterValuePair>

object ClearURLs {
    @Serializable
    data class ClearURLsProvider(
        /** all patterns starts with https? */
        val urlPattern: RegexAsString,
        val completeProvider: Boolean = false,
        val rules: List<RegexAsString>? = null,
        val rawRules: List<RegexAsString>? = null,
        val referralMarketing: List<String>? = null,
        val exceptions: List<RegexAsString>? = null,
        val redirections: List<RegexAsString>? = null,
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
            assetManager.open("data.min.json").bufferedReader().readText()
        )["providers"]
    }

    private val querySanitizer = UrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
    }

    fun transform(text: String): String {
        var x = text
        catalog?.let { map ->
            for ((_, provider) in map) {
                // matches url pattern
                if (provider.urlPattern.find(x) == null)
                    continue
                // not in exceptions
                if (provider.exceptions?.any { it.find(x) != null } == true)
                    continue
                // apply redirections
                provider.redirections?.forEach { redirection ->
                    redirection.find(x)?.let { matchResult ->
                        matchResult.groupValues.takeIf { it.size > 1 }?.let {
                            x = decodeURL(it[1])
                        }
                    }
                }
                provider.rawRules?.forEach { rawRule ->
                    x = rawRule.replace(x, "")
                }
                // apply rules (if any)
                val result = provider.rules.takeIf { !it.isNullOrEmpty() }
                    ?.let { rules ->
                        val uri = Uri.parse(x)
                        val newQuery = parseParams(uri.query).filterBy(rules).join()
                        val newFragment = parseParams(uri.fragment).filterBy(rules).join()
                        uri.buildUpon().encodedQuery(newQuery).fragment(newFragment).toString()
                    } ?: x
                Log.d("ClearURLs", "$text -> $result")
                return result
            }
            // no matching rules
            return x
        } ?: throw IllegalStateException("Catalog is unavailable")
    }

    private fun decodeURL(url: String) =
        URLDecoder.decode(url.replace("+", "%2B"), "UTF-8").replace("%2B", "+")

    private fun parseParams(str: String?): SearchParams {
        if (str.isNullOrEmpty()) return emptyList()
        querySanitizer.parseQuery(str)
        return querySanitizer.parameterList
    }

    private fun SearchParams.filterBy(rules: List<Regex>) = filter { param ->
        rules.all { !it.matches(param.mParameter) }
    }

    private fun SearchParam.join() = "${mParameter}=${mValue}"

    private fun SearchParams.join() = joinToString("&") { it.join() }
}
