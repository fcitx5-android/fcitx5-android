package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.BooleanKey.ClipboardEmpty
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.Clipboard
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.ClipboardTimedOut
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.Empty
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.Toolbar
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.ToolbarWithClip
import org.fcitx.fcitx5.android.utils.BuildTransitionEvent
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.TransitionBuildBlock

object IdleUiStateMachine {
    enum class State {
        Clipboard, Toolbar, Empty, ToolbarWithClip, ClipboardTimedOut
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        ClipboardEmpty
    }

    enum class TransitionEvent(val builder: TransitionBuildBlock<State, BooleanKey>) :
        EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        Timeout({
            from(ToolbarWithClip) transitTo Toolbar
            from(Clipboard) transitTo ClipboardTimedOut
        }),
        Pasted({
            accept { initial, current, _ ->
                when (current) {
                    Clipboard -> initial
                    ClipboardTimedOut -> initial
                    else -> current
                }
            }
        }),
        MenuButtonClicked({
            from(Toolbar) transitTo Empty
            from(ToolbarWithClip) transitTo Clipboard
            from(Clipboard) transitTo ToolbarWithClip
            from(ClipboardTimedOut) transitTo Toolbar
            from(Empty) transitTo Toolbar
        }),
        ClipboardUpdated({
            accept { initial, current, bool ->
                val clipboardEmpty = bool(ClipboardEmpty) == true
                when (current) {
                    ToolbarWithClip -> if (clipboardEmpty) Toolbar else Clipboard
                    Clipboard -> if (clipboardEmpty) initial else current
                    ClipboardTimedOut -> if (clipboardEmpty) initial else Clipboard
                    Toolbar -> if (!clipboardEmpty) Clipboard else current
                    Empty -> if (!clipboardEmpty) Clipboard else current
                }
            }
        }),
        KawaiiBarShown({
            accept { initial, current, _ ->
                if (current == ClipboardTimedOut) initial
                else current
            }
        })
    }

    fun new(toolbarByDefault: Boolean, block: (State) -> Unit) =
        EventStateMachine<State, TransitionEvent, BooleanKey>(if (toolbarByDefault) Toolbar else Empty).apply {
            onNewStateListener = block
        }

}
