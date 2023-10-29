/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.CandidateEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.PreeditEmpty
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.*
import org.fcitx.fcitx5.android.utils.BuildTransitionEvent
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.TransitionBuildBlock

object KawaiiBarStateMachine {
    enum class State {
        Idle, Candidate, Title
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        PreeditEmpty, CandidateEmpty
    }

    enum class TransitionEvent(val builder: TransitionBuildBlock<State, BooleanKey>) :
        EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        PreeditUpdated({
            from(Candidate) transitTo Idle on (PreeditEmpty to true)
            from(Idle) transitTo Candidate onF {
                it(PreeditEmpty) == false && it(CandidateEmpty) == false
            }
        }),
        CandidatesUpdated({
            from(Idle) transitTo Candidate on (CandidateEmpty to false)
            from(Candidate) transitTo Idle onF {
                it(PreeditEmpty) == true && it(CandidateEmpty) == true
            }
        }),
        ExtendedWindowAttached({
            from(Idle) transitTo Title
            from(Candidate) transitTo Title
        }),
        WindowDetached({
            // candidate state has higher priority so here it goes first
            from(Title) transitTo Candidate on (CandidateEmpty to false)
            from(Title) transitTo Idle
        }),
    }

    fun new(block: (State) -> Unit) =
        EventStateMachine<State, TransitionEvent, BooleanKey>(
            Idle
        ).apply {
            onNewStateListener = block
        }

}

