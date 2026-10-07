/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

/**
 * Pure decision logic for hold-to-talk on the toolbar microphone button, kept free of
 * Android view dependencies so it stays unit-testable.
 */
object HoldToTalkGesture {

    /**
     * A press shorter than this is treated as an accidental tap, not an attempt to
     * dictate: releasing it aborts the session instead of asking the recognizer to
     * transcribe (near-)empty audio.
     */
    const val MIN_HOLD_MS = 300L

    enum class ReleaseAction { Stop, Cancel }

    /**
     * What to do when the button is released: [ReleaseAction.Stop] finalizes the
     * session with whatever was recognized; [ReleaseAction.Cancel] aborts it.
     * A cancelled gesture (view stole the events, input view going away) always
     * aborts, as does a too-short hold.
     */
    fun releaseAction(heldMs: Long, gestureCancelled: Boolean): ReleaseAction =
        if (gestureCancelled || heldMs < MIN_HOLD_MS) ReleaseAction.Cancel else ReleaseAction.Stop
}
