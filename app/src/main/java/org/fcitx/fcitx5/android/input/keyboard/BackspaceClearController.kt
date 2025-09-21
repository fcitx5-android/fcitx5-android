/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.view.View
import org.fcitx.fcitx5.android.data.InputFeedbacks
import kotlin.math.absoluteValue

/**
 * Encapsulates the clear-confirm gesture flow for Backspace key.
 *
 * State machine:
 * - Idle -> Pending (onDown): schedule delayed show
 * - Pending -> Shown (timer fires) or -> Shown (immediate on vertical-dominant move)
 * - Shown: update focus with finger coordinates; red inside bubble, white otherwise
 * - Up: if Shown then trigger() (only clears when red), then dismiss(); return to Idle
 */
class BackspaceClearController(
    private val view: KeyView,
    private val showDelayMs: Long = 300L,
    private val callbacks: Callbacks,
    private val haptics: (View) -> Unit = { v -> InputFeedbacks.hapticFeedback(v, longPress = true) },
) {

    interface Callbacks {
        fun showClearConfirm(danger: Boolean)
        fun changeFocus(x: Float, y: Float): Boolean
        fun trigger(): Boolean
        fun dismiss()
        fun setRepeatEnabled(enabled: Boolean)
    }

    private enum class State { Idle, Pending, Shown }
    private var state: State = State.Idle
    private var pendingShow: Runnable? = null

    private fun cancelPending() {
        pendingShow?.let { view.removeCallbacks(it) }
        pendingShow = null
    }

    fun onDown() {
        callbacks.setRepeatEnabled(false)
        cancelPending()
        state = State.Pending
        pendingShow = Runnable {
            callbacks.showClearConfirm(false)
            state = State.Shown
        }
        view.postDelayed(pendingShow!!, showDelayMs)
    }

    /**
     * @return whether the move event is consumed
     */
    fun onMove(totalX: Int, totalY: Int, x: Float, y: Float): Boolean {
        val verticalDominant = totalY <= -1 && totalY.absoluteValue >= totalX.absoluteValue
        if (verticalDominant && state != State.Shown) {
            cancelPending()
            callbacks.showClearConfirm(true)
            haptics(view)
            state = State.Shown
            return true
        }
        if (state == State.Shown) {
            return callbacks.changeFocus(x, y)
        }
        return false
    }

    /**
     * @return whether the up event is consumed
     */
    fun onUp(): Boolean {
        cancelPending()
        val consumed = if (state == State.Shown) callbacks.trigger() else false
        // Dismiss is handled by onPopupTrigger() for consistency across popups.
        callbacks.setRepeatEnabled(true)
        state = State.Idle
        return consumed
    }
}
