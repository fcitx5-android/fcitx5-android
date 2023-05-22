package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef

sealed class PopupAction {

    abstract val viewId: Int

    data class PreviewAction(
        override val viewId: Int,
        val content: String,
        val bounds: Rect
    ) : PopupAction()

    data class PreviewUpdateAction(
        override val viewId: Int,
        val content: String,
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
