package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine

object IdleUiStateMachine {
    enum class State : EventStateMachine.State {
        Clipboard, Toolbar, Empty, ToolbarWithClip, ClipboardTimedOut
    }

    enum class TransitionEvent : EventStateMachine.StateTransitionEvent {
        Timeout,
        Pasted,
        MenuButtonClicked,
        ClipboardUpdatedEmpty,
        ClipboardUpdatedNonEmpty,
        KawaiiBarShown,
    }

    fun new(block: (State) -> Unit): EventStateMachine<State, TransitionEvent> =
        eventStateMachine(Empty) {
            from(Toolbar) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Toolbar) transitTo Empty on MenuButtonClicked
            from(ToolbarWithClip) transitTo Toolbar on Timeout
            from(ToolbarWithClip) transitTo Toolbar on ClipboardUpdatedEmpty
            from(ToolbarWithClip) transitTo Clipboard on MenuButtonClicked
            from(ToolbarWithClip) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Clipboard) transitTo ToolbarWithClip on MenuButtonClicked
            from(Clipboard) transitTo ClipboardTimedOut on Timeout
            from(Clipboard) transitTo Empty on Pasted
            from(ClipboardTimedOut) transitTo Toolbar on MenuButtonClicked
            from(ClipboardTimedOut) transitTo Empty on KawaiiBarShown
            from(Empty) transitTo Toolbar on MenuButtonClicked
            from(Empty) transitTo Clipboard on ClipboardUpdatedNonEmpty
            onNewState(block)
        }
}

