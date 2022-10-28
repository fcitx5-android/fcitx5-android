package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.*
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.TransitionEvent.*
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.eventStateMachine

object IdleUiStateMachine {
    enum class State {
        Clipboard, Toolbar, Empty, ToolbarWithClip, ClipboardTimedOut
    }

    enum class TransitionEvent {
        Timeout,
        Pasted,
        MenuButtonClicked,
        ClipboardUpdatedEmpty,
        ClipboardUpdatedNonEmpty,
        KawaiiBarShown,
    }

    fun new(
        toolbarByDefault: Boolean,
        old: EventStateMachine<State, TransitionEvent>? = null,
        block: ((State) -> Unit)? = null
    ): EventStateMachine<State, TransitionEvent> {
        val initialState = if (toolbarByDefault) Toolbar else Empty
        return eventStateMachine(old?.currentState ?: initialState) {
            from(Toolbar) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Toolbar) transitTo Empty on MenuButtonClicked
            from(ToolbarWithClip) transitTo Toolbar on Timeout
            from(ToolbarWithClip) transitTo Toolbar on ClipboardUpdatedEmpty
            from(ToolbarWithClip) transitTo Clipboard on MenuButtonClicked
            from(ToolbarWithClip) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Clipboard) transitTo ToolbarWithClip on MenuButtonClicked
            from(Clipboard) transitTo ClipboardTimedOut on Timeout
            from(Clipboard) transitTo initialState on Pasted
            from(Clipboard) transitTo initialState on ClipboardUpdatedEmpty
            from(ClipboardTimedOut) transitTo Toolbar on MenuButtonClicked
            from(ClipboardTimedOut) transitTo initialState on KawaiiBarShown
            from(ClipboardTimedOut) transitTo initialState on Pasted
            from(ClipboardTimedOut) transitTo initialState on ClipboardUpdatedEmpty
            from(ClipboardTimedOut) transitTo Clipboard on ClipboardUpdatedNonEmpty
            from(Empty) transitTo Toolbar on MenuButtonClicked
            from(Empty) transitTo Clipboard on ClipboardUpdatedNonEmpty
            onNewState(old?.onNewStateListener ?: block ?: throw IllegalArgumentException())
        }
    }
}
