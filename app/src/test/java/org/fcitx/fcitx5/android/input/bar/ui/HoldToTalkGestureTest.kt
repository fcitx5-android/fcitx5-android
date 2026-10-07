/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

import org.fcitx.fcitx5.android.input.bar.ui.HoldToTalkGesture.ReleaseAction
import org.fcitx.fcitx5.android.input.bar.ui.HoldToTalkGesture.MIN_HOLD_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class HoldToTalkGestureTest {

    @Test
    fun `a solid hold stops the session`() {
        assertEquals(ReleaseAction.Stop, HoldToTalkGesture.releaseAction(MIN_HOLD_MS, false))
        assertEquals(ReleaseAction.Stop, HoldToTalkGesture.releaseAction(10_000L, false))
    }

    @Test
    fun `an accidental short tap cancels instead of recognizing empty audio`() {
        assertEquals(ReleaseAction.Cancel, HoldToTalkGesture.releaseAction(0L, false))
        assertEquals(ReleaseAction.Cancel, HoldToTalkGesture.releaseAction(MIN_HOLD_MS - 1, false))
    }

    @Test
    fun `a cancelled gesture always aborts`() {
        assertEquals(ReleaseAction.Cancel, HoldToTalkGesture.releaseAction(10_000L, true))
    }
}
