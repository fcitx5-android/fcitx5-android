package org.fcitx.fcitx5.android.input.bar

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
            from(Candidate) transitTo Idle on { it(BooleanKey.CandidateEmpty) == true }
            from(Idle) transitTo Candidate on { it(BooleanKey.CandidateEmpty) == false }
        }),
        CandidatesUpdated({
            from(Idle) transitTo Candidate on { it(BooleanKey.CandidateEmpty) == false }
        }),
        ExtendedWindowAttached({
            from(Idle) transitTo Title
            from(Candidate) transitTo Title
            from(NumberRow) transitTo Title
        }),
        CapFlagsUpdated({
            from(Idle) transitTo NumberRow on { it(BooleanKey.CapFlagsPassword) == true }
            from(NumberRow) transitTo Idle on { it(BooleanKey.CapFlagsPassword) == false }
        }),
        WindowDetached({
            // candidate state has higher priority so here it goes first
            from(Title) transitTo Candidate on {
                it(
                    BooleanKey.CandidateEmpty
                ) == false
            }
            from(Title) transitTo Idle on { it(BooleanKey.CapFlagsPassword) == false }
            from(Title) transitTo NumberRow on { it(BooleanKey.CapFlagsPassword) == true }
        }),
        KeyboardSwitchedOutNumber({
            from(Idle) transitTo NumberRow on { it(BooleanKey.CapFlagsPassword) == true }
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

