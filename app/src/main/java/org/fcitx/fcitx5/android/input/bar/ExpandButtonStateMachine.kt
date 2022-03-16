package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine


object ExpandButtonStateMachine {
    enum class State : EventStateMachine.State {
        ClickToAttachWindow,
        ClickToDetachWindow,
        Hidden
    }

    enum class TransitionEvent : EventStateMachine.StateTransitionEvent {
        ExpandedCandidatesUpdatedEmpty,
        ExpandedCandidatesUpdatedNonEmpty,
        ExpandedCandidatesAttached,
        ExpandedCandidatesDetachedWithEmpty,
        ExpandedCandidatesDetachedWithNonEmpty,
    }

    fun new(block: (State) -> Unit): EventStateMachine<State, TransitionEvent> =
        eventStateMachine(
            Hidden
        ) {
            from(Hidden) transitTo ClickToAttachWindow on ExpandedCandidatesUpdatedNonEmpty
            from(ClickToAttachWindow) transitTo Hidden on ExpandedCandidatesUpdatedEmpty
            from(ClickToAttachWindow) transitTo ClickToDetachWindow on ExpandedCandidatesAttached
            from(ClickToDetachWindow) transitTo ClickToAttachWindow on ExpandedCandidatesDetachedWithNonEmpty
            from(ClickToDetachWindow) transitTo Hidden on ExpandedCandidatesDetachedWithEmpty
            onNewState(block)
        }
}