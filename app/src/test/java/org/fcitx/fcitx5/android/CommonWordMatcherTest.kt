/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.data.quickphrase.CommonWordMatcher
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommonWordMatcherTest {
    private val address = QuickPhraseEntry(
        "address",
        "北京市朝阳区示例路100号"
    )

    @Test
    fun completesPhraseAfterThreeOrMoreMatchingCharacters() {
        val three = CommonWordMatcher.bestMatch("收件地址：北京市", listOf(address))!!
        assertEquals("北京市", three.matchedPrefix)
        assertEquals("朝阳区示例路100号", three.completion)

        val longer = CommonWordMatcher.bestMatch("北京市朝阳区", listOf(address))!!
        assertEquals("北京市朝阳区", longer.matchedPrefix)
        assertEquals("示例路100号", longer.completion)
    }

    @Test
    fun doesNotSuggestBeforeThreeCharactersOrAfterFullPhrase() {
        assertNull(CommonWordMatcher.bestMatch("北京", listOf(address)))
        assertNull(CommonWordMatcher.bestMatch(address.phrase, listOf(address)))
        assertNull(CommonWordMatcher.bestMatch("这是无关内容", listOf(address)))
    }

    @Test
    fun choosesLongestCurrentPrefixAndSupportsSupplementaryCharacters() {
        val short = QuickPhraseEntry("short", "北京市海淀区")
        val emoji = QuickPhraseEntry("emoji", "😀😁😂常用表情")

        assertEquals(
            address,
            CommonWordMatcher.bestMatch("北京市朝阳", listOf(short, address))?.entry
        )
        val match = CommonWordMatcher.bestMatch("😀😁😂", listOf(emoji))!!
        assertEquals("常用表情", match.completion)
    }

    @Test
    fun asciiMatchingIsCaseInsensitiveButPreservesStoredCompletion() {
        val account = QuickPhraseEntry("account", "Account-Example-001")
        val match = CommonWordMatcher.bestMatch("account", listOf(account))!!
        assertEquals("-Example-001", match.completion)
    }
}
