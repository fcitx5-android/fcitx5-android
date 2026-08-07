/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

object CommonWordMatcher {
    const val MIN_PREFIX_CODE_POINTS = 3

    data class Match(
        val entry: QuickPhraseEntry,
        val matchedPrefix: String,
        val completion: String
    )

    fun bestMatch(
        textBeforeCursor: String,
        entries: List<QuickPhraseEntry>,
        minPrefixCodePoints: Int = MIN_PREFIX_CODE_POINTS
    ): Match? = entries
        .mapNotNull { match(textBeforeCursor, it, minPrefixCodePoints) }
        .maxByOrNull { it.matchedPrefix.codePointCount(0, it.matchedPrefix.length) }

    private fun match(
        textBeforeCursor: String,
        entry: QuickPhraseEntry,
        minPrefixCodePoints: Int
    ): Match? {
        val phrase = entry.phrase
        val phraseCodePoints = phrase.codePointCount(0, phrase.length)
        if (phraseCodePoints <= minPrefixCodePoints) return null
        val beforeCodePoints = textBeforeCursor.codePointCount(0, textBeforeCursor.length)
        val maxPrefix = minOf(phraseCodePoints - 1, beforeCodePoints)
        for (prefixLength in maxPrefix downTo minPrefixCodePoints) {
            val prefixEnd = phrase.offsetByCodePoints(0, prefixLength)
            val prefix = phrase.substring(0, prefixEnd)
            if (textBeforeCursor.endsWith(prefix, ignoreCase = true)) {
                return Match(entry, prefix, phrase.substring(prefixEnd))
            }
        }
        return null
    }
}
