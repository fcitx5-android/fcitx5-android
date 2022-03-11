package me.rocka.fcitx5test.input.bar

import me.rocka.fcitx5test.utils.NaiveStateMachine

enum class KawaiiBarState : NaiveStateMachine.State {
    Idle, Candidate, Title
}