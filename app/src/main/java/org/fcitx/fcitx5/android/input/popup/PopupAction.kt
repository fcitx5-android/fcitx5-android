/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef

sealed class PopupAction {

    abstract val viewId: Int

    enum class PreviewStyle {
        // Legacy tall preview bubble centered above key
        Default,
        // Wide preview placed above the key (scaled, distinct from Default)
        WideAbove,
    }

    data class PreviewAction(
        override val viewId: Int,
        val content: String,
        val bounds: Rect,
        val style: PreviewStyle = PreviewStyle.Default
    ) : PopupAction()

    data class PreviewUpdateAction(
        override val viewId: Int,
        val content: String,
    ) : PopupAction()

    /**
     * Update preview popup visual state (e.g., danger hint vs normal).
     */
    data class PreviewDangerHintAction(
        override val viewId: Int,
        val danger: Boolean
    ) : PopupAction()

    data class DismissAction(
        override val viewId: Int
    ) : PopupAction()

    data class ShowKeyboardAction(
        override val viewId: Int,
        val keyboard: KeyDef.Popup.Keyboard,
        val bounds: Rect
    ) : PopupAction()

    data class ShowMenuAction(
        override val viewId: Int,
        val menu: KeyDef.Popup.Menu,
        val bounds: Rect
    ) : PopupAction()

    /**
     * Show the dedicated clear-confirm container above Backspace.
     * [danger] indicates whether to start in danger hint state (i.e., equivalent
     * to the previous "armed" visual when user already swiped upward dominantly).
     */
    data class ShowClearConfirmAction(
        override val viewId: Int,
        val bounds: Rect,
        val danger: Boolean,
    ) : PopupAction()

    data class ChangeFocusAction(
        override val viewId: Int,
        val x: Float,
        val y: Float,
        var outResult: Boolean = false
    ) : PopupAction()

    data class TriggerAction(
        override val viewId: Int,
        var outAction: KeyAction? = null
    ) : PopupAction()
}
