package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine


object ExpandButtonStateMachine {
    enum class State {
        ClickToAttachWindow,
        ClickToDetachWindow,
        Hidden
    }

    enum class TransitionEvent {
        ExpandedCandidatesUpdatedEmpty,
        ExpandedCandidatesUpdatedNonEmpty,
        ExpandedCandidatesAttached,
        ExpandedCandidatesDetachedWithCandidatesEmpty,
        ExpandedCandidatesDetachedWithCandidatesNonEmpty,
    }

    fun new(block: (State) -> Unit): EventStateMachine<State, TransitionEvent> =
        eventStateMachine(
            Hidden
        ) {
            from(Hidden) transitTo ClickToAttachWindow on ExpandedCandidatesUpdatedNonEmpty
            from(ClickToAttachWindow) transitTo Hidden on ExpandedCandidatesUpdatedEmpty
            from(ClickToAttachWindow) transitTo ClickToDetachWindow on ExpandedCandidatesAttached
            from(ClickToDetachWindow) transitTo ClickToAttachWindow on ExpandedCandidatesDetachedWithCandidatesNonEmpty
            from(ClickToDetachWindow) transitTo Hidden on ExpandedCandidatesDetachedWithCandidatesEmpty
            onNewState(block)
        }
}