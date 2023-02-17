package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.CandidateEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.CapFlagsPassword
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Candidate
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Idle
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.NumberRow
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Title
import org.fcitx.fcitx5.android.utils.BuildTransitionEvent
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.TransitionBuildBlock

object KawaiiBarStateMachine {
    enum class State {
        Idle, Candidate, Title, NumberRow
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        CandidateEmpty, CapFlagsPassword
    }

    enum class TransitionEvent(val builder: TransitionBuildBlock<State, BooleanKey>) :
        EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        PreeditUpdatedNonEmpty({
            from(Candidate) transitTo Idle on (CandidateEmpty to true)
            from(Idle) transitTo Candidate on (CandidateEmpty to false)
        }),
        CandidatesUpdated({
            from(Idle) transitTo Candidate on (CandidateEmpty to false)
        }),
        ExtendedWindowAttached({
            from(Idle) transitTo Title
            from(Candidate) transitTo Title
            from(NumberRow) transitTo Title
        }),
        CapFlagsUpdated({
            from(Idle) transitTo NumberRow on (CapFlagsPassword to true)
            from(NumberRow) transitTo Idle on (CapFlagsPassword to false)
        }),
        WindowDetached({
            // candidate state has higher priority so here it goes first
            from(Title) transitTo Candidate on (CandidateEmpty to false)
            from(Title) transitTo Idle on (CapFlagsPassword to false)
            from(Title) transitTo NumberRow on (CapFlagsPassword to true)
        }),
        KeyboardSwitchedOutNumber({
            from(Idle) transitTo NumberRow on (CapFlagsPassword to true)
        }),
        KeyboardSwitchedToNumber({
            from(NumberRow) transitTo Idle
        })
    }

    fun new(block: (State) -> Unit) =
        EventStateMachine<State, TransitionEvent, BooleanKey>(
            Idle
        ).apply {
            onNewStateListener = block
        }

}

