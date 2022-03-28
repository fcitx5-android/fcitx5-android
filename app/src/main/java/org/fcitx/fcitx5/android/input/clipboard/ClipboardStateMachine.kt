package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.*
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine

object ClipboardStateMachine {

    enum class State {
        Normal, AddMore, EnableListening
    }

    enum class TransitionEvent {
        ClipboardDbUpdatedEmpty,
        ClipboardDbUpdatedNonEmpty,
        ClipboardListeningDisabled,
        ClipboardListeningEnabledWithDbNonEmpty,
        ClipboardListeningEnabledWithDbEmpty
    }

    fun new(
        initialState: State,
        block: (State) -> Unit
    ): EventStateMachine<State, TransitionEvent> = eventStateMachine(
        initialState
    ) {
        from(Normal) transitTo AddMore on ClipboardDbUpdatedEmpty
        from(Normal) transitTo EnableListening on ClipboardListeningDisabled
        from(EnableListening) transitTo Normal on ClipboardListeningEnabledWithDbNonEmpty
        from(EnableListening) transitTo AddMore on ClipboardListeningEnabledWithDbEmpty
        from(AddMore) transitTo Normal on ClipboardDbUpdatedNonEmpty
        from(AddMore) transitTo EnableListening on ClipboardListeningDisabled
        onNewState(block)
    }
}