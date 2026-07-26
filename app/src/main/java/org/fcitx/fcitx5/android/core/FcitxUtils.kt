/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.core

object FcitxUtils {

    // https://github.com/fcitx/fcitx5/blob/5.1.21/src/lib/fcitx-utils/stringutils.cpp#L100
    // https://en.cppreference.com/cpp/language/escape
    private val unescapeMap = mapOf(
        '\\' to '\\',
        '"' to '"',
        'n' to '\n',
        'f' to '\u000c',
        'r' to '\r',
        't' to '\t',
        'v' to '\u000b',
    )

    // https://github.com/fcitx/fcitx5/blob/5.1.21/src/lib/fcitx-utils/stringutils.cpp#L390
    fun unescapeForValue(str: String): String {
        if (str.length > 2 && str.startsWith('"') && str.endsWith('"')) {
            val (consumed, result) = consumeMaybeEscapedValue(str)
            return if (consumed.length == str.length) result else ""
        }
        return str
    }

    // https://en.cppreference.com/cpp/string/basic_string/find_first_of
    private fun String.findFirstOf(str: String, pos: Int = 0): Int {
        val set = str.toSet()
        for (i in pos..<length) {
            if (set.contains(get(i))) {
                return i
            }
        }
        return -1
    }
    // https://en.cppreference.com/cpp/string/basic_string/find_first_not_of
    private fun String.findFirstNotOf(str: String, pos: Int = 0): Int {
        val set = str.toSet()
        for (i in pos..<length) {
            if (!set.contains(get(i))) {
                return i
            }
        }
        return -1
    }

    enum class UnescapeState { NORMAL, ESCAPE }

    // https://github.com/fcitx/fcitx5/blob/5.1.21/src/lib/fcitx-utils/stringutils.cpp#L435
    private fun consumeMaybeEscapedValue(str: String, skip: String = ""): Pair<String, String> {
        var input = str
        val start = input.findFirstNotOf(skip)
        if (start < 0) {
            return "" to ""
        }
        input = input.substring(start)
        val maybeQuoted = input.startsWith('"')
        if (maybeQuoted) {
            var end = 0
            val result = buildString {
                var state = UnescapeState.NORMAL
                input.forEachIndexed { i, c ->
                    // skip first "
                    if (i == 0) {
                        return@forEachIndexed
                    }
                    when (state) {
                        UnescapeState.NORMAL -> {
                            when (c) {
                                '\\' -> {
                                    state = UnescapeState.ESCAPE
                                }
                                '"' -> {
                                    end = i + 1
                                    return@buildString
                                }
                                else -> {
                                    append(c)
                                }
                            }
                        }
                        UnescapeState.ESCAPE -> {
                            // treat invalid escape sequence as normal character
                            append(unescapeMap[c] ?: c)
                            state = UnescapeState.NORMAL
                        }
                    }
                }
            }
            if (end > 0) {
                val consumed = input.substring(0, end)
                return consumed to result
            }
        }
        val end = input.findFirstOf(skip, 1)
        val consumed = if (end < 0) input else input.substring(0, end)
        return consumed to consumed
    }

    // https://github.com/fcitx/fcitx5/blob/5.1.21/src/lib/fcitx-utils/stringutils.cpp#L86
    private val escapeMap = mapOf(
        '\\' to '\\',
        '"' to '"',
        '\n' to 'n',
        '\u000c' to 'f',
        '\r' to 'r',
        '\t' to 't',
        '\u000b' to 'v',
    )

    // "\f\r\t\v \"\\\n"
    private val EscapeChars = charArrayOf('\u000c', '\r', '\t', '\u000b', ' ', '"', '\\', '\n')

    // https://github.com/fcitx/fcitx5/blob/5.1.21/src/lib/fcitx-utils/stringutils.cpp#L404
    fun escapeForValue(str: String): String {
        val needsEscape = str.lastIndexOfAny(EscapeChars) >= 0
        return buildString {
            if (needsEscape) append('"')
            str.forEach { c ->
                val escape = escapeMap[c]
                if (escape != null) {
                    append('\\')
                    append(escape)
                } else {
                    append(c)
                }
            }
            if (needsEscape) append('"')
        }
    }
}
