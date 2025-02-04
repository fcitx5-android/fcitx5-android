/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
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

typealias RegexAsString = @Serializable(with = RegexSerializer::class) Regex

object ClearURLs {
    @Serializable
    data class ClearURLsProvider(
        /** all patterns starts with https? */
        val urlPattern: RegexAsString,
        val completeProvider: Boolean = false,
        val rules: List<RegexAsString> = emptyList(),
        val rawRules: List<RegexAsString> = emptyList(),
        val referralMarketing: List<RegexAsString> = emptyList(),
        val exceptions: List<RegexAsString> = emptyList(),
        val redirections: List<RegexAsString> = emptyList(),
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

    private val urlPattern = Regex("^https?://", RegexOption.IGNORE_CASE)

    fun transform(text: String): String {
        if (!urlPattern.matchesAt(text, 0))
            return text
        var x = text
        var matched = false
        catalog?.let { map ->
            for ((_, provider) in map) {
                // matches url pattern
                if (!provider.urlPattern.containsMatchIn(x))
                    continue
                // not in exceptions
                if (provider.exceptions.any { it.containsMatchIn(x) })
                    continue
                matched = true
                // apply redirections
                provider.redirections.forEach { redirection ->
                    redirection.matchEntire(x)?.groupValues?.getOrNull(1)?.let {
                        x = decodeURL(it)
                        log(if (BuildConfig.DEBUG) "$text ~> $x" else "(redirect)")
                        return x
                    }
                }
                provider.rawRules.forEach { rawRule ->
                    x = rawRule.replace(x, "")
                }
                /**
                 * apply rules and referralMarketing
                 * https://docs.clearurls.xyz/1.26.1/specs/rules/#referralmarketing
                 * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/clearurls.js#L449
                 */
                val rules = provider.rules + provider.referralMarketing
                val uri = Uri.parse(x)
                x = uri.buildUpon()
                    .encodedQuery(filterParams(uri.query, rules))
                    /**
                     * clear #fragments too
                     * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/clearurls.js#L109
                     * but skip URL encode because of reasons
                     * https://github.com/ClearURLs/Addon/blob/d8da43ac297e5df51a9c7276579ac3332adfa801/core_js/utils/URLHashParams.js#L65
                     */
                    .encodedFragment(filterParams(uri.fragment, rules, encode = false))
                    .toString()
            }
            if (matched) {
                log(if (BuildConfig.DEBUG) "$text -> $x" else "(clear)")
            }
            return x
        } ?: throw IllegalStateException("Catalog is unavailable")
    }

    /**
     * mimic the js impl
     * https://github.com/ClearURLs/Addon/blob/deec80b763179fa5c3559a37e3c9a6f1b28d0886/core_js/tools.js#L243
     */
    private fun decodeURL(str: String): String {
        var a: String
        var b: String = str
        do {
            a = b
            b = Uri.decode(b)
        } while (a != b)
        return b
    }

    private fun encodeQuery(str: String) = Uri.encode(str, " ").replace(" ", "+")

    private val querySanitizer = UrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        unregisteredParameterValueSanitizer = UrlQuerySanitizer.getAllButNulLegal()
    }

    private fun UrlQuerySanitizer.ParameterValuePair.stringify(encode: Boolean = true): String {
        val k = if (encode) encodeQuery(mParameter) else mParameter
        if (mValue.isEmpty()) return k
        val v = if (encode) encodeQuery(mValue) else mValue
        return "$k=$v"
    }

    private fun filterParams(params: String?, rules: List<Regex>, encode: Boolean = true): String? {
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
            .joinToString("&") { it.stringify(encode) }
    }

    private fun log(msg: String) {
        Log.d("ClearURLs", msg)
    }
}
