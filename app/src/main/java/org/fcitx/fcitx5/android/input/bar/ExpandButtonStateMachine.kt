package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.BooleanKey.CandidatesEmpty
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.ClickToAttachWindow
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.ClickToDetachWindow
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.Hidden
import org.fcitx.fcitx5.android.utils.BuildTransitionEvent
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.TransitionBuildBlock


object ExpandButtonStateMachine {
    enum class State {
        ClickToAttachWindow,
        ClickToDetachWindow,
        Hidden
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        CandidatesEmpty
    }

    enum class TransitionEvent(val builder: TransitionBuildBlock<State, BooleanKey>) :
        EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        ExpandedCandidatesUpdated({
            from(Hidden) transitTo ClickToAttachWindow on (CandidatesEmpty to false)
            from(ClickToAttachWindow) transitTo Hidden on (CandidatesEmpty to true)
        }),
        ExpandedCandidatesAttached({
            from(ClickToAttachWindow) transitTo ClickToDetachWindow
        }),
        ExpandedCandidatesDetached({
            from(ClickToDetachWindow) transitTo Hidden on (CandidatesEmpty to true)
            from(ClickToDetachWindow) transitTo ClickToAttachWindow on (CandidatesEmpty to false)
        });
    }

    fun new(block: (State) -> Unit) =
        EventStateMachine<State, TransitionEvent, BooleanKey>(Hidden).apply {
            onNewStateListener = block
        }
}

