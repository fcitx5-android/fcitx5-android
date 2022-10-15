package org.fcitx.fcitx5.android.input.keyboard

import androidx.lifecycle.lifecycleScope
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.daemon.launchOnFcitxReady
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dialog.AddMoreInputMethodsPrompt
import org.fcitx.fcitx5.android.input.dialog.InputMethodSwitcherDialog
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.*
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.*
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import org.fcitx.fcitx5.android.utils.inputConnection
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class CommonKeyActionListener :
    UniqueComponent<CommonKeyActionListener>(), Dependent, ManagedHandler by managedHandler() {

    enum class BackspaceSwipeState {
        Stopped, Selection, Reset
    }

    private val context by manager.context()
    private val fcitx by manager.fcitx()
    private val service by manager.inputMethodService()
    private val inputView by manager.inputView()
    private val preedit: PreeditComponent by manager.must()

    private var backspaceSwipeState = Stopped

    val listener by lazy {
        KeyActionListener { action, _ ->
            service.lifecycleScope.launchOnFcitxReady(fcitx) {
                when (action) {
                    is FcitxKeyAction -> it.sendKey(action.act, KeyState.Virtual.state)
                    is SymAction -> it.sendKey(action.sym, action.states)
                    is CommitAction -> {
                        if (preedit.content.preedit.run { preedit.isEmpty() && clientPreedit.isEmpty() }) {
                            // preedit is empty, there can be prediction candidates
                            it.reset()
                        } else if (it.inputMethodEntryCached.uniqueName == "keyboard-us") {
                            // androidkeyboard clears composing on reset, but we want to commit it as-is
                            service.inputConnection?.finishComposingText()
                            it.reset()
                        } else {
                            if (!it.select(0)) it.reset()
                        }
                        service.inputConnection?.commitText(action.text, 1)
                    }
                    is QuickPhraseAction -> {
                        it.reset()
                        it.triggerQuickPhrase()
                    }
                    is UnicodeAction -> {
                        it.reset()
                        it.triggerUnicode()
                    }
                    is LangSwitchAction -> {
                        if (it.enabledIme().size < 2) {
                            inputView.showDialog(
                                AddMoreInputMethodsPrompt.build(
                                    service,
                                    context
                                )
                            )
                        } else {
                            it.enumerateIme()
                        }
                    }
                    is InputMethodSwitchAction -> {
                        inputView.showDialog(
                            InputMethodSwitcherDialog.build(it, service, context)
                        )
                    }
                    is MoveSelectionAction -> when (backspaceSwipeState) {
                        Stopped -> backspaceSwipeState = preedit.content.preedit.let { p ->
                            if (p.preedit.isEmpty() && p.clientPreedit.isEmpty()) {
                                // update state to `Selection` and apply first offset
                                service.applySelectionOffset(action.start, action.end)
                                Selection
                            } else {
                                Reset
                            }
                        }
                        Selection -> {
                            service.applySelectionOffset(action.start, action.end)
                        }
                        Reset -> {}
                    }
                    is DeleteSelectionAction -> {
                        when (backspaceSwipeState) {
                            Stopped -> {}
                            Selection -> if (service.selection.isNotEmpty()) {
                                service.inputConnection?.commitText("", 1)
                            }
                            Reset -> if (action.totalCnt < 0) { // swipe left
                                it.reset()
                            }
                        }
                        backspaceSwipeState = Stopped
                    }
                    else -> {
                    }
                }
            }
        }
    }
}
