package org.fcitx.fcitx5.android.common.clearurls

import android.net.Uri
import android.net.UrlQuerySanitizer
import org.json.JSONArray
import org.json.JSONObject

class ClearUrlsRuleFilter(rawRules: String) {
    private data class Provider(
        val urlPattern: Regex,
        val rules: List<Regex> = emptyList(),
        val rawRules: List<Regex> = emptyList(),
        val referralMarketing: List<Regex> = emptyList(),
        val exceptions: List<Regex> = emptyList(),
        val redirections: List<Regex> = emptyList()
    )

    private val catalog = parseProviders(rawRules)
    private val urlPattern = Regex("^https?://", RegexOption.IGNORE_CASE)

    fun transform(text: String): String {
        if (!urlPattern.matchesAt(text, 0)) return text
        var result = text
        for (provider in catalog) {
            if (!provider.urlPattern.containsMatchIn(result)) continue
            if (provider.exceptions.any { it.containsMatchIn(result) }) continue
            provider.redirections.forEach { redirection ->
                redirection.matchAt(result, 0)?.groupValues?.getOrNull(1)?.let {
                    return decodeUrl(it)
                }
            }
            provider.rawRules.forEach { rule ->
                result = rule.replace(result, "")
            }
            val rules = provider.rules + provider.referralMarketing
            val uri = Uri.parse(result)
            result = uri.buildUpon()
                .encodedQuery(filterParams(uri.query, rules))
                .encodedFragment(filterParams(uri.fragment, rules, encode = false))
                .toString()
        }
        return result
    }

    private fun parseProviders(rawRules: String): List<Provider> {
        val providers = JSONObject(rawRules)
            .optJSONObject("providers")
            ?: return emptyList()
        val result = mutableListOf<Provider>()
        val keys = providers.keys()
        while (keys.hasNext()) {
            val provider = providers.optJSONObject(keys.next()) ?: continue
            val pattern = provider.optString("urlPattern")
            if (pattern.isEmpty()) continue
            result += Provider(
                urlPattern = pattern.toRegexSafe(),
                rules = provider.optRegexList("rules"),
                rawRules = provider.optRegexList("rawRules"),
                referralMarketing = provider.optRegexList("referralMarketing"),
                exceptions = provider.optRegexList("exceptions"),
                redirections = provider.optRegexList("redirections")
            )
        }
        return result
    }

    private fun JSONObject.optRegexList(key: String): List<Regex> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                array.optString(i)?.takeIf { it.isNotEmpty() }?.let { add(it.toRegexSafe()) }
            }
        }
    }

    private fun String.toRegexSafe(): Regex =
        Regex(this, RegexOption.IGNORE_CASE)

    private fun decodeUrl(str: String): String {
        var a: String
        var b = str
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
        val key = if (encode) encodeQuery(mParameter) else mParameter
        if (mValue.isEmpty()) return key
        val value = if (encode) encodeQuery(mValue) else mValue
        return "$key=$value"
    }

    private fun filterParams(params: String?, rules: List<Regex>, encode: Boolean = true): String? {
        if (params.isNullOrEmpty()) return params
        querySanitizer.parseQuery(params)
        return querySanitizer.parameterList
            .filter { param -> rules.all { !it.matches(param.mParameter) } }
            .joinToString("&") { it.stringify(encode) }
    }
}
