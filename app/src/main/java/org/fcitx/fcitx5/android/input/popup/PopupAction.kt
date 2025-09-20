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
        // Same size as key and fully placed above it
        FitAbove,
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
     * Update preview popup visual state (e.g., armed vs normal).
     * When armed = true, preview may use a more prominent style.
     */
    data class PreviewArmedAction(
        override val viewId: Int,
        val armed: Boolean
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
