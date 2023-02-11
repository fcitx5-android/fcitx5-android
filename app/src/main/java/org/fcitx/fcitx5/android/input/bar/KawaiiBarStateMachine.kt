package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Candidate
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Idle
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.NumberRow
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Title
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.CandidateUpdateNonEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.CapFlagsUpdatedNoPassword
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.CapFlagsUpdatedPassword
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.ExtendedWindowAttached
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.PreeditUpdatedEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.PreeditUpdatedNonEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.WindowDetachedWithCandidatesEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.WindowDetachedWithCandidatesNonEmpty
import org.fcitx.fcitx5.android.utils.eventStateMachine

object KawaiiBarStateMachine {
    enum class State {
        Idle, Candidate, Title, NumberRow
    }

    enum class TransitionEvent {
        PreeditUpdatedEmpty,
        PreeditUpdatedNonEmpty,
        CandidateUpdateNonEmpty,
        ExtendedWindowAttached,
        WindowDetachedWithCandidatesEmpty,
        WindowDetachedWithCandidatesNonEmpty,
        CapFlagsUpdatedPassword,
        CapFlagsUpdatedNoPassword,
    }

    fun new(block: (State) -> Unit) = eventStateMachine(Idle) {
        from(Idle) transitTo Title on ExtendedWindowAttached
        from(Idle) transitTo Candidate on PreeditUpdatedNonEmpty
        from(Idle) transitTo Candidate on CandidateUpdateNonEmpty
        from(Title) transitTo Idle on WindowDetachedWithCandidatesEmpty
        from(Title) transitTo Candidate on WindowDetachedWithCandidatesNonEmpty
        from(Candidate) transitTo Idle on PreeditUpdatedEmpty
        from(Candidate) transitTo Title on ExtendedWindowAttached
        from(Idle) transitTo NumberRow on CapFlagsUpdatedPassword
        from(NumberRow) transitTo Idle on CapFlagsUpdatedNoPassword
        onNewState(block)
    }
}

