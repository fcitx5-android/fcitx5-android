/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

/**
 * Maps `onRmsChanged` values into 0..1 for the wave animation.
 *
 * Implementations use two common conventions: Google's default service reports roughly
 * [-2, +10], while offline services typically report negative dBFS values. The convention
 * is detected once per session and mapped with fixed absolute ranges:
 *
 * - dBFS: -50 dB (room silence) maps to 0; -5 dB (loud speech) maps to 1
 * - Google-style: -2 maps to 0 and +10 maps to 1
 */
class SpeechVolumeNormalizer {

    private enum class Mode { UNKNOWN, DBFS, GOOGLE }

    private var mode = Mode.UNKNOWN

    fun reset() {
        mode = Mode.UNKNOWN
    }

    fun normalize(db: Float): Float {
        if (db.isNaN()) return 0f
        detectMode(db)
        return when (mode) {
            Mode.GOOGLE -> ((db - GOOGLE_MIN) / (GOOGLE_MAX - GOOGLE_MIN)).coerceIn(0f, 1f)
            else -> ((db - DBFS_MIN) / (DBFS_MAX - DBFS_MIN)).coerceIn(0f, 1f)
        }
    }

    private fun detectMode(db: Float) {
        if (mode != Mode.UNKNOWN) return
        mode = when {
            db > GOOGLE_MAX -> Mode.GOOGLE
            db > DBFS_MAX -> Mode.GOOGLE
            else -> Mode.DBFS
        }
    }

    private companion object {
        const val DBFS_MIN = -50f
        const val DBFS_MAX = -5f
        const val GOOGLE_MIN = -2f
        const val GOOGLE_MAX = 10f
    }
}
