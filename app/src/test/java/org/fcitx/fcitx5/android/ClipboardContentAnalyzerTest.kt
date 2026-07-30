/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.clipboard.ClipboardContentAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardContentAnalyzerTest {
    @Test
    fun classifiesSupportedContent() {
        val cases = listOf(
            "+86 138-0013-8000" to ClipboardCategory.Phone,
            "person+tag@example.com" to ClipboardCategory.Email,
            "https://example.com/path?q=1" to ClipboardCategory.Url,
            "example.org/docs" to ClipboardCategory.Url,
            "1234" to ClipboardCategory.Otp,
            "123456" to ClipboardCategory.Otp,
            "12345678" to ClipboardCategory.Phone,
            "１２３４５６" to ClipboardCategory.Otp,
            "复制这段内容后打开淘宝 ￥AbC123xy￥" to ClipboardCategory.TrackingToken,
            "￥AbC123xy￥" to ClipboardCategory.TrackingToken,
            "ordinary clipboard text" to ClipboardCategory.Other
        )

        cases.forEach { (text, expected) ->
            assertEquals(text, expected, ClipboardContentAnalyzer.analyze(text).category)
        }
    }

    @Test
    fun avoidsAmbiguousFalsePositives() {
        val cases = listOf(
            "12345",
            "order 123456",
            "验证码：123456",
            "Verification code: 8042",
            "验证码说明中没有数字",
            "call me at 12345",
            "email person@example.com in this sentence",
            "visit https://example.com for details",
            "这里提到了口令但没有结构",
            "复制这段内容后打开淘宝领取优惠"
        )

        cases.forEach { text ->
            assertEquals(text, ClipboardCategory.Other, ClipboardContentAnalyzer.analyze(text).category)
        }
    }

    @Test
    fun trackingTokenTakesPriorityOverEmbeddedDigits() {
        assertEquals(
            ClipboardCategory.TrackingToken,
            ClipboardContentAnalyzer.analyze("验证码 123456，复制后打开淘宝 ￥AbC123xy￥").category
        )
        assertEquals(ClipboardCategory.Other, ClipboardContentAnalyzer.analyze("OTP: 123456").category)
    }
}
