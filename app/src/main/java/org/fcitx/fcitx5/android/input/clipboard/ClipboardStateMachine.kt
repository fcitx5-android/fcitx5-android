package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.*
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine
import org.fcitx.fcitx5.android.utils.times

object ClipboardStateMachine {

    enum class State : EventStateMachine.State {
        Normal, AddMore, EnableListening
    }

    enum class TransitionEvent : EventStateMachine.StateTransitionEvent {
        ClipboardDbUpdatedEmpty,
        ClipboardDbUpdatedNonEmpty,
        ClipboardListeningDisabled,
        ClipboardListeningEnabled
    }

    fun new(
        initialState: State,
        block: (State) -> Unit
    ): EventStateMachine<State, TransitionEvent> = eventStateMachine(
        initialState
    ) {
        from(Normal) transitTo AddMore on ClipboardDbUpdatedEmpty
        from(Normal) transitTo EnableListening on ClipboardListeningDisabled
        from(EnableListening) transitTo Normal on ClipboardListeningEnabled * ClipboardDbUpdatedNonEmpty
        from(EnableListening) transitTo AddMore on ClipboardListeningEnabled * ClipboardDbUpdatedEmpty
        from(AddMore) transitTo Normal on ClipboardDbUpdatedNonEmpty
        from(AddMore) transitTo EnableListening on ClipboardListeningDisabled
        onNewState(block)
    }
}