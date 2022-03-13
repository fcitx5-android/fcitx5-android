package me.rocka.fcitx5test.input.bar

import me.rocka.fcitx5test.utils.EventStateMachine

enum class KawaiiBarTransitionEvent : EventStateMachine.StateTransitionEvent {
    PreeditUpdatedEmpty,
    PreeditUpdatedNonEmpty,
    ExtendedWindowAttached,
    WindowDetached
}