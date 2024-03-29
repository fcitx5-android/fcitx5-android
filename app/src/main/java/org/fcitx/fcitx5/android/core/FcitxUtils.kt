/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.core

object FcitxUtils {

    // https://github.com/fcitx/fcitx5/blob/5.1.8/src/lib/fcitx-utils/stringutils.cpp#L323
    // https://github.com/fcitx/fcitx5/blob/5.1.8/src/lib/fcitx-utils/stringutils.cpp#L362
    fun unescapeForValue(str: String): String {
        val quoted = str.length >= 2 && str.first() == '"' && str.last() == '"'
        val s = if (quoted) str.substring(1, str.length - 1) else str
        if (s.isEmpty()) return s
        var escape = false
        return buildString {
            s.forEach { c ->
                when (escape) {
                    false -> {
                        if (c == '\\') {
                            escape = true
                        } else {
                            append(c)
                        }
                    }
                    true -> {
                        if (c == '\\') {
                            append('\\')
                        } else if (c == 'n') {
                            append('\n')
                        } else if (c == '"' && quoted) {
                            append('"')
                        } else {
                            throw IllegalStateException("Unexpected escape sequence '\\${c}' when unescaping string '${str}'.")
                        }
                        escape = false
                    }
                }
            }
        }
    }

    private val QuotedChars = charArrayOf(' ', '"', '\t', '\r', '\u000b', '\u000c')

    // https://github.com/fcitx/fcitx5/blob/5.1.8/src/lib/fcitx-utils/stringutils.cpp#L380
    fun escapeForValue(str: String): String {
        val needsQuote = str.lastIndexOfAny(QuotedChars) >= 0
        return buildString {
            if (needsQuote) append('"')
            str.forEach { c ->
                append(
                    when (c) {
                        '\\' -> "\\\\"
                        '\n' -> "\\n"
                        '"' -> "\\\""
                        else -> c
                    }
                )
            }
            if (needsQuote) append('"')
        }
    }
}
