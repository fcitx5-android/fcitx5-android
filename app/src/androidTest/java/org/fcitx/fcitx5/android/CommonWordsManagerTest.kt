/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseEntry
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CommonWordsManagerTest {
    @Test
    fun commonWordsUseQuickPhraseStorageAndPublishUpdates() {
        val original = QuickPhraseManager.loadCommonWords()
        val updates = mutableListOf<List<QuickPhraseEntry>>()
        val listener = QuickPhraseManager.OnCommonWordsChangedListener {
            updates.add(it)
        }
        val address = QuickPhraseEntry("address", "北京市朝阳区示例路100号")
        val account = QuickPhraseEntry("account", "Account-Example-001")

        QuickPhraseManager.addOnCommonWordsChangedListener(listener)
        try {
            QuickPhraseManager.saveCommonWords(listOf(address, account))
            assertEquals(listOf(address, account), QuickPhraseManager.loadCommonWords())
            assertEquals(listOf(address, account), updates.last())

            assertEquals(
                listOf(account),
                QuickPhraseManager.deleteCommonWord(address)
            )
            assertEquals(listOf(account), QuickPhraseManager.loadCommonWords())
            assertEquals(listOf(account), updates.last())
        } finally {
            QuickPhraseManager.removeOnCommonWordsChangedListener(listener)
            QuickPhraseManager.saveCommonWords(original)
        }
    }
}
