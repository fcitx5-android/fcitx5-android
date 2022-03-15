package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.utils.EventStateMachine

enum class ExpandButtonState : EventStateMachine.State {
    ClickToAttachWindow,
    ClickToDetachWindow,
    Hidden
}