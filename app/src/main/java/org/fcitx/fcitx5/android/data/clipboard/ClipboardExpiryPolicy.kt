/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

object ClipboardExpiryPolicy {
    const val MILLIS_PER_MINUTE = 60_000L
    const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE

    data class Settings(
        val otpEnabled: Boolean,
        val otpMinutes: Int,
        val trackingTokenEnabled: Boolean,
        val trackingTokenHours: Int
    )

    fun expiresAt(
        category: ClipboardCategory,
        copiedAt: Long,
        pinned: Boolean,
        favorite: Boolean,
        settings: Settings
    ): Long? {
        if (pinned || favorite) return null
        val lifetime = when (category) {
            ClipboardCategory.Otp ->
                if (settings.otpEnabled) settings.otpMinutes * MILLIS_PER_MINUTE else null
            ClipboardCategory.TrackingToken ->
                if (settings.trackingTokenEnabled) {
                    settings.trackingTokenHours * MILLIS_PER_HOUR
                } else {
                    null
                }
            else -> null
        }
        return lifetime?.let { copiedAt + it }
    }
}
