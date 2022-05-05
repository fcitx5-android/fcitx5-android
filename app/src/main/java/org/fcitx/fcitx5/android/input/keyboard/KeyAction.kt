package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym

sealed class KeyAction {

    data class FcitxKeyAction(var act: String) : KeyAction() {
        fun upper() {
            act = act.uppercase()
        }

        fun lower() {
            act = act.lowercase()
        }
    }

    data class SymAction(val sym: KeySym, val states: KeyStates = VirtualState) : KeyAction() {
        constructor(sym: UInt, states: KeyStates = VirtualState) : this(KeySym(sym), states)

        companion object {
            val VirtualState = KeyStates(KeyState.Virtual)
        }
    }

    data class CapsAction(val lock: Boolean) : KeyAction()

    object QuickPhraseAction : KeyAction()

    object UnicodeAction : KeyAction()

    object LangSwitchAction : KeyAction()

    object InputMethodSwitchAction : KeyAction()

    data class LayoutSwitchAction(val act: String = "") : KeyAction()

    class MoveSelectionAction(val start: Int = 0, val end: Int = 0) : KeyAction()

    object DeleteSelectionAction : KeyAction()

    data class PopupPreviewAction(
        val viewId: Int,
        val character: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val dismiss: Boolean = false
    ) : KeyAction() {
        constructor(character: String) : this(0, character, 0, 0, 0, 0)

        fun withCoordinate(viewId: Int, left: Int, top: Int, right: Int, bottom: Int) =
            copy(viewId = viewId, left = left, top = top, right = right, bottom = bottom)

        fun asDismiss(viewId: Int) = copy(viewId = viewId, dismiss = true)
    }
}
