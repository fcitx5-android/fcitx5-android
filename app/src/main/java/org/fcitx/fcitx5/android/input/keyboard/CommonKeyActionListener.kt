package org.fcitx.fcitx5.android.input.keyboard

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.*
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.inputConnection
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import splitties.systemservices.inputMethodManager

class CommonKeyActionListener :
    UniqueComponent<CommonKeyActionListener>(), Dependent, ManagedHandler by managedHandler() {

    enum class BackspaceSwipeState {
        Stopped, Selection, Reset
    }

    private val context by manager.context()
    private val fcitx by manager.fcitx()
    private val service by manager.inputMethodService()
    private val preedit: PreeditComponent by manager.must()

    private var backspaceSwipeState = Stopped

    val listener by lazy {
        BaseKeyboard.KeyActionListener { action ->
            service.lifecycleScope.launch {
                when (action) {
                    is KeyAction.FcitxKeyAction -> fcitx.sendKey(action.act, KeyState.Virtual.state)
                    is KeyAction.SymAction -> fcitx.sendKey(action.sym, action.states)
                    is KeyAction.QuickPhraseAction -> {
                        fcitx.reset()
                        fcitx.triggerQuickPhrase()
                    }
                    is KeyAction.UnicodeAction -> {
                        fcitx.reset()
                        fcitx.triggerUnicode()
                    }
                    is KeyAction.LangSwitchAction -> {
                        if (fcitx.enabledIme().size < 2) {
                            AppUtil.launchMainToAddInputMethods(context)
                        } else {
                            fcitx.enumerateIme()
                        }
                    }
                    is KeyAction.InputMethodSwitchAction -> inputMethodManager.showInputMethodPicker()
                    is KeyAction.MoveSelectionAction -> when (backspaceSwipeState) {
                        Stopped -> backspaceSwipeState = preedit.content.preedit.let {
                            if (it.preedit.isEmpty() && it.clientPreedit.isEmpty()) {
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
                    is KeyAction.DeleteSelectionAction -> {
                        when (backspaceSwipeState) {
                            Stopped -> {}
                            Selection -> if (service.selection.isNotEmpty()) {
                                service.inputConnection?.commitText("", 1)
                            }
                            Reset -> if (action.totalCnt < 0) { // swipe left
                                fcitx.reset()
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