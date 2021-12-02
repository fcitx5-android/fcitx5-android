package me.rocka.fcitx5test.keyboard.layout

import me.rocka.fcitx5test.native.Fcitx

sealed class ButtonAction<T> {

    abstract val act: T

    data class FcitxKeyAction(override val act: String) : ButtonAction<String>()

    data class CommitAction(override val act: String) : ButtonAction<String>()

    object CapsAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object BackspaceAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object QuickPhraseAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object UnicodeAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object LangSwitchAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object InputMethodSwitchAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object ReturnAction : ButtonAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    data class LayoutSwitchAction(override val act: String = "") : ButtonAction<String>()

    data class CustomAction(override val act: (Fcitx) -> Unit) : ButtonAction<(Fcitx) -> Unit>()

}