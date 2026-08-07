/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.clipboard.ClipboardExpiryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardExpiryPolicyTest {
    private val enabled = ClipboardExpiryPolicy.Settings(
        otpEnabled = true,
        otpMinutes = 10,
        trackingTokenEnabled = true,
        trackingTokenHours = 24
    )

    @Test
    fun schedulesOnlyEnabledShortLivedCategories() {
        assertEquals(
            601_000L,
            ClipboardExpiryPolicy.expiresAt(
                ClipboardCategory.Otp, 1_000, false, false, enabled
            )
        )
        assertEquals(
            86_401_000L,
            ClipboardExpiryPolicy.expiresAt(
                ClipboardCategory.TrackingToken, 1_000, false, false, enabled
            )
        )
        ClipboardCategory.entries
            .filterNot { it == ClipboardCategory.Otp || it == ClipboardCategory.TrackingToken }
            .forEach {
                assertNull(
                    ClipboardExpiryPolicy.expiresAt(it, 1_000, false, false, enabled)
                )
            }
    }

    @Test
    fun disabledSettingsAreExactlyNoOp() {
        val disabled = enabled.copy(
            otpEnabled = false,
            trackingTokenEnabled = false
        )
        assertNull(
            ClipboardExpiryPolicy.expiresAt(
                ClipboardCategory.Otp, 1_000, false, false, disabled
            )
        )
        assertNull(
            ClipboardExpiryPolicy.expiresAt(
                ClipboardCategory.TrackingToken, 1_000, false, false, disabled
            )
        )
    }

    @Test
    fun pinnedAndFavoriteEntriesNeverExpire() {
        assertNull(
            ClipboardExpiryPolicy.expiresAt(
                ClipboardCategory.Otp, 1_000, pinned = true, favorite = false, enabled
            )
        )
        assertNull(
            ClipboardExpiryPolicy.expiresAt(
                ClipboardCategory.TrackingToken,
                1_000,
                pinned = false,
                favorite = true,
                enabled
            )
        )
    }
}
