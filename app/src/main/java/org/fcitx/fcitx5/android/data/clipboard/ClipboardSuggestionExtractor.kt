/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

enum class ClipboardSuggestionKind { Link, Email, Phone, Number }

data class ClipboardSuggestion(val kind: ClipboardSuggestionKind, val text: String)

/** Temporary paste targets extracted from clipboard text, without changing clipboard history. */
object ClipboardSuggestionExtractor {
    private const val MAX_SCANNABLE_CHARS = 16_384
    private const val DEFAULT_LIMIT = 100

    // Exclude email domains; URLs may have no scheme.
    private val urlPattern = Regex(
        """(?i)(?<![\p{L}\p{N}_@])(?:https?://)?(?:[\p{L}\p{N}](?:[\p{L}\p{N}-]*[\p{L}\p{N}])?\.)+[\p{L}]{2,}(?::\d{1,5})?(?:[/?#][^\s<>"'“”‘’，。；！？、]*)?"""
    )
    private val emailPattern = Regex(
        """(?i)(?<![\p{L}\p{N}_+.-])[\p{L}\p{N}_%+.-]+@(?:[\p{L}\p{N}](?:[\p{L}\p{N}-]*[\p{L}\p{N}])?\.)+[\p{L}]{2,}(?![\p{L}\p{N}_-])"""
    )
    // Common mobile, formatted North American, and explicit international numbers only.
    private val phonePatterns = listOf(
        Regex("""(?<![\p{L}\p{N}])(?:\+?86[\s-]?)?1[3-9]\d{9}(?![\p{L}\p{N}])"""),
        Regex("""(?<![\p{L}\p{N}])(?:\+1[\s.-]?)?(?:\([2-9]\d{2}\)[\s.-]?|[2-9]\d{2}[\s.-])[2-9]\d{2}[\s.-]\d{4}(?![\p{L}\p{N}])"""),
        Regex("""(?<![\p{L}\p{N}])\+\d{1,3}(?:[\s().-]*\d){7,14}(?![\p{L}\p{N}])""")
    )
    private val numberPattern = Regex("""(?<![\p{L}\p{N}_])\d{4,}(?![\p{L}\p{N}_])""")
    private val trailingPunctuation = ".,;:!?，。；：！？、）】》」』)]}"

    private data class Found(
        val start: Int,
        val endExclusive: Int,
        val suggestion: ClipboardSuggestion
    )

    fun extract(text: String, limit: Int = DEFAULT_LIMIT): List<ClipboardSuggestion> {
        if (limit <= 0 || text.length > MAX_SCANNABLE_CHARS) return emptyList()
        val matches = mutableListOf<Found>()
        val patterns = listOf(
            ClipboardSuggestionKind.Link to urlPattern,
            ClipboardSuggestionKind.Email to emailPattern,
            *phonePatterns.map { ClipboardSuggestionKind.Phone to it }.toTypedArray(),
            ClipboardSuggestionKind.Number to numberPattern
        )
        for ((kind, pattern) in patterns) {
            for (match in pattern.findAll(text)) {
                val value = if (kind == ClipboardSuggestionKind.Link) {
                    match.value.stripTrailingPunctuation()
                } else match.value
                if (value.isEmpty()) continue
                if (kind == ClipboardSuggestionKind.Phone && value.count(Char::isDigit) !in 8..15) continue
                matches.add(Found(match.range.first, match.range.first + value.length, ClipboardSuggestion(kind, value)))
            }
        }

        val original = text.trim()
        val claimed = mutableListOf<Found>()
        val results = mutableListOf<ClipboardSuggestion>()
        val seen = HashSet<String>()
        for (match in matches.sortedWith(
            compareBy<Found> { it.start }
                .thenByDescending { it.endExclusive }
                .thenBy { it.suggestion.kind.ordinal }
        )) {
            if (claimed.any { it.start < match.endExclusive && match.start < it.endExclusive }) continue
            if (!seen.add(match.suggestion.text)) continue
            // A full clipboard item still claims its span, so digits inside a copied URL
            // or phone number never become a misleading extra suggestion.
            claimed.add(match)
            if (match.suggestion.text == original) continue
            results.add(match.suggestion)
            if (results.size >= limit) break
        }
        return results
    }

    private fun String.stripTrailingPunctuation(): String {
        var end = length
        while (end > 0 && this[end - 1] in trailingPunctuation) {
            val last = this[end - 1]
            if (last == ')' && substring(0, end).count { it == ')' } <= substring(0, end).count { it == '(' }) break
            if (last == ']' && substring(0, end).count { it == ']' } <= substring(0, end).count { it == '[' }) break
            end--
        }
        return substring(0, end)
    }
}
