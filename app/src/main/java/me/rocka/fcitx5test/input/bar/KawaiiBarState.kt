package me.rocka.fcitx5test.input.bar

import me.rocka.fcitx5test.utils.EventStateMachine

enum class KawaiiBarState : EventStateMachine.State {
    Idle, Candidate, Title
}