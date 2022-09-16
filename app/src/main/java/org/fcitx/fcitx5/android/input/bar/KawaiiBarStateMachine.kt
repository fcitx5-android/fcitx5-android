package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.eventStateMachine

object KawaiiBarStateMachine {
    enum class State {
        Idle, Candidate, Title
    }

    enum class TransitionEvent {
        PreeditUpdatedEmpty,
        PreeditUpdatedNonEmpty,
        CandidateUpdateNonEmpty,
        ExtendedWindowAttached,
        WindowDetachedWithCandidatesEmpty,
        WindowDetachedWithCandidatesNonEmpty
    }

    fun new(block: (State) -> Unit) = eventStateMachine(Idle) {
        from(Idle) transitTo Title on ExtendedWindowAttached
        from(Idle) transitTo Candidate on PreeditUpdatedNonEmpty
        from(Idle) transitTo Candidate on CandidateUpdateNonEmpty
        from(Title) transitTo Idle on WindowDetachedWithCandidatesEmpty
        from(Title) transitTo Candidate on WindowDetachedWithCandidatesNonEmpty
        from(Candidate) transitTo Idle on PreeditUpdatedEmpty
        from(Candidate) transitTo Title on ExtendedWindowAttached
        onNewState(block)
    }
}

