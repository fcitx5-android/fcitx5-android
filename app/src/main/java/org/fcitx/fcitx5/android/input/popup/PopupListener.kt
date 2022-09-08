package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef

interface PopupListener {
    fun onPreview(viewId: Int, content: String, bounds: Rect)
    fun onPreviewUpdate(viewId: Int, content: String)
    fun onDismiss(viewId: Int)
    fun onShowKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect)
    fun onChangeFocus(viewId: Int, x: Float, y: Float): Boolean
    fun onTriggerKeyboard(viewId: Int): KeyAction?
}
