/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import android.content.ClipData
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.utils.clipboardManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardExpiryManagerTest {
    @Test
    fun otpExpiryFollowsSettingsAndProtection() = runBlocking {
        val prefs = AppPrefs.getInstance().clipboard
        val originalEnabled = prefs.clipboardOtpAutoDelete.getValue()
        val originalMinutes = prefs.clipboardOtpAutoDeleteMinutes.getValue()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        try {
            prefs.clipboardOtpAutoDeleteMinutes.setValue(1)
            prefs.clipboardOtpAutoDelete.setValue(true)
            ClipboardManager.nukeTable()

            val copiedAt = System.currentTimeMillis()
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("", "654321"))
            ClipboardManager.onPrimaryClipChanged()
            val inserted = awaitEntry("654321")

            assertEquals(ClipboardCategory.Otp, inserted.category)
            assertNotNull(inserted.expiresAt)
            assertTrue(inserted.expiresAt!! in (copiedAt + 55_000)..(copiedAt + 70_000))

            ClipboardManager.favorite(inserted.id)
            assertNull(ClipboardManager.get(inserted.id)?.expiresAt)
            ClipboardManager.unfavorite(inserted.id)
            assertNull(ClipboardManager.get(inserted.id)?.expiresAt)

            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("", "654321"))
            ClipboardManager.onPrimaryClipChanged()
            assertNotNull(awaitEntry("654321", requireExpiry = true).expiresAt)

            prefs.clipboardOtpAutoDelete.setValue(false)
            withTimeout(5_000) {
                while (ClipboardManager.get(inserted.id)?.expiresAt != null) delay(50)
            }

            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("", "1234"))
            ClipboardManager.onPrimaryClipChanged()
            assertNull(awaitEntry("1234").expiresAt)
        } finally {
            prefs.clipboardOtpAutoDelete.setValue(originalEnabled)
            prefs.clipboardOtpAutoDeleteMinutes.setValue(originalMinutes)
            ClipboardManager.nukeTable()
        }
    }

    private suspend fun awaitEntry(
        text: String,
        requireExpiry: Boolean = false
    ): ClipboardEntry = withTimeout(5_000) {
        while (true) {
            val entry = ClipboardManager.lastEntry
            if (entry?.text == text && (!requireExpiry || entry.expiresAt != null)) {
                return@withTimeout entry
            }
            delay(50)
        }
        error("unreachable")
    }
}
