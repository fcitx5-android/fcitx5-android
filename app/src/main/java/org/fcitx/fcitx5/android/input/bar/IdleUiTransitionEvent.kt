package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.utils.EventStateMachine

enum class IdleUiTransitionEvent : EventStateMachine.StateTransitionEvent {
    Timeout,
    Pasted,
    MenuButtonClickedWithClipboardNonEmpty,
    MenuButtonClickedWithClipboardEmpty,
    ClipboardUpdatedNonEmpty,
}