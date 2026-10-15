/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechVolumeNormalizerTest {

    @Test
    fun `dbfs silence maps to zero and loud speech to one`() {
        val n = SpeechVolumeNormalizer()
        assertEquals(0f, n.normalize(-60f), 1e-6f)
        assertEquals(0f, n.normalize(-50f), 1e-6f)
        assertEquals(1f, n.normalize(-2f), 1e-6f)
        assertEquals(0.5f, n.normalize(-27.5f), 0.01f)
    }

    @Test
    fun `google convention maps to the unit range`() {
        val n = SpeechVolumeNormalizer()
        assertEquals(1f, n.normalize(12f), 1e-6f)
        assertEquals(0f, n.normalize(-3f), 1e-6f)
        assertEquals(0.5f, n.normalize(4f), 0.01f)
    }

    @Test
    fun `output is clamped and NaN-safe`() {
        val n = SpeechVolumeNormalizer()
        assertEquals(0f, n.normalize(Float.NaN), 1e-6f)
        assertTrue(n.normalize(-100f) in 0f..1f)
        assertTrue(n.normalize(100f) in 0f..1f)
    }

    @Test
    fun `reset forgets the detected convention`() {
        val n = SpeechVolumeNormalizer()
        n.normalize(8f) // google mode
        assertEquals(1f, n.normalize(12f), 1e-6f)
        n.reset()
        n.normalize(-30f) // dbfs mode again
        assertEquals(0f, n.normalize(-55f), 1e-6f)
    }
}
