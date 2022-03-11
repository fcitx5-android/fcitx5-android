package me.rocka.fcitx5test.input.bar

import me.rocka.fcitx5test.utils.EventStateMachine

enum class ExpandButtonTransitionEvent : EventStateMachine.StateTransitionEvent {
    ExpandedCandidatesUpdatedEmpty,
    ExpandedCandidatesUpdatedNonEmpty,
    ExpandedCandidatesAttached,
    ExpandedCandidatesDetachedWithEmpty,
    ExpandedCandidatesDetachedWithNonEmpty,
}