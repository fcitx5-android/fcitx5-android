package me.rocka.fcitx5test.keyboard.layout

import me.rocka.fcitx5test.native.Fcitx

sealed class ButtonAction<T>(open val act: T) {

    class FcitxKeyAction(override val act: String) : ButtonAction<String>(act)

    class CommitAction(override val act: String) : ButtonAction<String>(act)

    class CapsAction : ButtonAction<Unit>(Unit)

    class BackspaceAction : ButtonAction<Unit>(Unit)

    class QuickPhraseAction : ButtonAction<Unit>(Unit)

    class UnicodeAction : ButtonAction<Unit>(Unit)

    class LangSwitchAction : ButtonAction<Unit>(Unit)

    class InputMethodSwitchAction : ButtonAction<Unit>(Unit)

    class ReturnAction : ButtonAction<Unit>(Unit)

    class CustomAction(override val act: (Fcitx) -> Unit) : ButtonAction<(Fcitx) -> Unit>(act)

}