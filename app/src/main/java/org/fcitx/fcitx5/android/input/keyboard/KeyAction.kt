package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym

sealed class KeyAction {

    data class FcitxKeyAction(var act: String) : KeyAction()

    data class SymAction(val sym: KeySym, val states: KeyStates = VirtualState) : KeyAction() {
        constructor(sym: UInt, states: KeyStates = VirtualState) : this(KeySym(sym), states)

        companion object {
            val VirtualState = KeyStates(KeyState.Virtual)
        }
    }

    data class CommitAction(var text: String) : KeyAction()

    data class CapsAction(val lock: Boolean) : KeyAction()

    object QuickPhraseAction : KeyAction()

    object UnicodeAction : KeyAction()

    object LangSwitchAction : KeyAction()

    object InputMethodSwitchAction : KeyAction()

    data class LayoutSwitchAction(val act: String = "") : KeyAction()

    data class MoveSelectionAction(val start: Int = 0, val end: Int = 0) : KeyAction()

    data class DeleteSelectionAction(val totalCnt: Int = 0) : KeyAction()
}
