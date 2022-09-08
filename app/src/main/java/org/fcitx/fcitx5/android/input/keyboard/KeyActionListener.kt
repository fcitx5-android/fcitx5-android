package org.fcitx.fcitx5.android.input.keyboard

fun interface KeyActionListener {

    enum class Source {
        Keyboard, Popup
    }

    fun onKeyAction(action: KeyAction, source: Source)
}
