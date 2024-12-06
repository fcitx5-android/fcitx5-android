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
import java.net.URLEncoder

typealias RegexAsString = @Serializable(with = RegexSerializer::class) Regex

object ClearURLs {
    @Serializable
    data class ClearURLsProvider(
        /** all patterns starts with https? */
        val urlPattern: RegexAsString,
        val completeProvider: Boolean = false,
        val rules: List<RegexAsString>? = null,
        val rawRules: List<RegexAsString>? = null,
        val referralMarketing: List<RegexAsString>? = null,
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

    fun transform(text: String): String {
        if (!text.startsWith("http"))
            return text
        var x = text
        var matched = false
        catalog?.let { map ->
            for ((_, provider) in map) {
                // matches url pattern
                if (!provider.urlPattern.containsMatchIn(x))
                    continue
                // not in exceptions
                if (provider.exceptions?.any { it.containsMatchIn(x) } == true)
                    continue
                matched = true
                // apply redirections
                provider.redirections?.forEach { redirection ->
                    redirection.matchEntire(x)?.groupValues?.getOrNull(1)?.let {
                        x = decodeURL(it)
                    }
                }
                provider.rawRules?.forEach { rawRule ->
                    x = rawRule.replace(x, "")
                }
                // apply rules (if any)
                if (provider.rules.isNullOrEmpty())
                    continue
                /**
                 * merge rules with referralMarketing
                 * https://docs.clearurls.xyz/1.26.1/specs/rules/#referralmarketing
                 * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/clearurls.js#L449
                 */
                val rules =
                    if (provider.referralMarketing.isNullOrEmpty()) provider.rules
                    else provider.rules + provider.referralMarketing
                val uri = Uri.parse(x)
                x = uri.buildUpon()
                    .encodedQuery(filterParams(uri.query, rules))
                    /**
                     * clear #fragments too
                     * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/clearurls.js#L109
                     */
                    .encodedFragment(filterParams(uri.fragment, rules))
                    .toString()
            }
            if (matched) {
                Log.d("ClearURLs", "$text -> $x")
            }
            return x
        } ?: throw IllegalStateException("Catalog is unavailable")
    }

    private const val UTF8 = "UTF-8"

    /**
     * mimic the js impl
     * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/core_js/tools.js#L243
     */
    private fun decodeURL(str: String): String {
        var a = str
        var b = URLDecoder.decode(str, UTF8)
        while (a != b) {
            a = b
            b = URLDecoder.decode(b, UTF8)
        }
        return b
    }

    private fun encodeURL(str: String) = URLEncoder.encode(str, UTF8)

    private val querySanitizer = UrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        unregisteredParameterValueSanitizer = UrlQuerySanitizer.getAllButNulLegal()
    }

    private fun filterParams(params: String?, rules: List<Regex>): String? {
        if (params.isNullOrEmpty()) return params
        querySanitizer.parseQuery(params)
        return querySanitizer.parameterList
            .filter { param ->
                /**
                 * match rules with search parameter keys
                 * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/clearurls.js#L122
                 */
                rules.all { !it.matches(param.mParameter) }
            }
            .joinToString("&") {
                if (it.mValue.isEmpty()) encodeURL(it.mParameter)
                else "${encodeURL(it.mParameter)}=${encodeURL(it.mValue)}"
            }
    }
}
