package me.rocka.fcitx5test.input.bar

import me.rocka.fcitx5test.utils.EventStateMachine

enum class IdleUiTransitionEvent : EventStateMachine.StateTransitionEvent {
    Timeout,
    Pasted,
    MenuButtonClickedWithClipboardNonEmpty,
    MenuButtonClickedWithClipboardEmpty,
    ClipboardUpdatedNonEmpty,
}