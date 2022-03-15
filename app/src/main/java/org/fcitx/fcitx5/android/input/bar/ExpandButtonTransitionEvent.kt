package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.utils.EventStateMachine

enum class ExpandButtonTransitionEvent : EventStateMachine.StateTransitionEvent {
    ExpandedCandidatesUpdatedEmpty,
    ExpandedCandidatesUpdatedNonEmpty,
    ExpandedCandidatesAttached,
    ExpandedCandidatesDetachedWithEmpty,
    ExpandedCandidatesDetachedWithNonEmpty,
}