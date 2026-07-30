/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

import java.net.URI
import java.text.Normalizer

object ClipboardContentAnalyzer {
    const val VERSION = 1

    data class Analysis(
        val category: ClipboardCategory,
        val ruleId: String
    )

    private val trackingToken = Regex("""(?s).*[￥¥₤][^￥¥₤\r\n]{4,128}[￥¥₤].*""")
    private val email = Regex(
        """(?i)^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$"""
    )
    private val bareUrl = Regex(
        """(?i)^(?:www\.)?(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(?::\d{1,5})?(?:[/?#][^\s]*)?$"""
    )
    private val phoneCharacters = Regex("""^\+?[0-9()\-\s]+$""")

    fun analyze(text: String): Analysis {
        val normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFKC)
        if (normalized.isEmpty()) return Analysis(ClipboardCategory.Other, "empty")

        if (trackingToken.matches(normalized)) {
            return Analysis(ClipboardCategory.TrackingToken, "tracking-token")
        }

        val exactDigits = normalized.all(Char::isDigit)
        if (exactDigits && normalized.length in setOf(4, 6)) {
            return Analysis(ClipboardCategory.Otp, "otp")
        }

        if (normalized.length <= 254 && email.matches(normalized)) {
            return Analysis(ClipboardCategory.Email, "email")
        }

        if (isUrl(normalized)) {
            return Analysis(ClipboardCategory.Url, "url")
        }

        if (phoneCharacters.matches(normalized)) {
            val digitCount = normalized.count(Char::isDigit)
            if (digitCount in 7..15) {
                return Analysis(ClipboardCategory.Phone, "phone")
            }
        }

        return Analysis(ClipboardCategory.Other, "other")
    }

    private fun isUrl(value: String): Boolean {
        if (bareUrl.matches(value)) return true
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() !in setOf("http", "https", "ftp")) return false
        return !uri.host.isNullOrBlank() && !value.any(Char::isWhitespace)
    }
}
