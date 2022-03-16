package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine
import org.fcitx.fcitx5.android.utils.times

object IdleUiStateMachine {
    enum class State : EventStateMachine.State {
        Clipboard, Toolbar, Empty
    }

    enum class TransitionEvent : EventStateMachine.StateTransitionEvent {
        Timeout,
        Pasted,
        MenuButtonClicked,
        ClipboardUpdatedEmpty,
        ClipboardUpdatedNonEmpty,
    }

    fun new(block: (State) -> Unit): EventStateMachine<State, TransitionEvent> =
        eventStateMachine(Empty) {
            from(Toolbar) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Toolbar) transitTo Clipboard on MenuButtonClicked * ClipboardUpdatedNonEmpty
            from(Toolbar) transitTo Empty on MenuButtonClicked * ClipboardUpdatedEmpty
            from(Clipboard) transitTo Toolbar on MenuButtonClicked* ClipboardUpdatedNonEmpty
            from(Clipboard) transitTo Empty on Timeout
            from(Clipboard) transitTo Empty on Pasted
            from(Empty) transitTo Toolbar on MenuButtonClicked * ClipboardUpdatedEmpty
            from(Empty) transitTo Clipboard on ClipboardUpdatedNonEmpty
            onNewState(block)
        }
}

