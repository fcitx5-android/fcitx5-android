/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.BooleanKey.ClipboardDbEmpty
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.BooleanKey.ClipboardListeningEnabled
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.AddMore
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.EnableListening
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine.State.Normal
import org.fcitx.fcitx5.android.utils.BuildTransitionEvent
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.TransitionBuildBlock

object ClipboardStateMachine {

    enum class State {
        Normal, AddMore, EnableListening
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        ClipboardDbEmpty,
        ClipboardListeningEnabled
    }

    enum class TransitionEvent(val builder: TransitionBuildBlock<State, BooleanKey>) :
        EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        ClipboardDbUpdated({
            from(Normal) transitTo AddMore on (ClipboardDbEmpty to true)
            from(AddMore) transitTo Normal on (ClipboardDbEmpty to false)
        }),
        ClipboardListeningUpdated({
            from(Normal) transitTo EnableListening on (ClipboardListeningEnabled to false)
            from(EnableListening) transitTo Normal onF {
                it(ClipboardListeningEnabled) == true && it(ClipboardDbEmpty) == false
            }
            from(EnableListening) transitTo AddMore onF {
                it(ClipboardListeningEnabled) == true && it(ClipboardDbEmpty) == true
            }
            from(AddMore) transitTo EnableListening on (ClipboardListeningEnabled to false)
        })
    }

    fun new(initialState: State, block: (State) -> Unit) =
        EventStateMachine<State, TransitionEvent, BooleanKey>(initialState).apply {
            onNewStateListener = block
        }

}