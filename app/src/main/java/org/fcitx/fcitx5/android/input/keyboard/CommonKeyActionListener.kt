package org.fcitx.fcitx5.android.input.keyboard

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.inputConnection
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.systemservices.inputMethodManager

class CommonKeyActionListener :
    UniqueComponent<CommonKeyActionListener>(), Dependent, ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val fcitx by manager.fcitx()
    private val service by manager.inputMethodService()

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
                    is KeyAction.MoveSelectionAction -> {
                        val current = service.selection
                        val start = current.start + action.start
                        val end = current.end + action.end
                        if (start > end) return@launch
                        service.inputConnection?.setSelection(start, end)
                    }
                    is KeyAction.DeleteSelectionAction -> {
                        if (service.selection.isNotEmpty()) {
                            service.inputConnection?.commitText("", 1)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }
}