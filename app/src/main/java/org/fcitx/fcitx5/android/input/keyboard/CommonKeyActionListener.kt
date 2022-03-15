package org.fcitx.fcitx5.android.input.keyboard

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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

class CommonKeyActionListener : UniqueComponent<CommonKeyActionListener>(), Dependent,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val fcitx by manager.fcitx()
    private val service by manager.inputMethodService()


    // TODO: We expose this listener share with expandable candidate.
    //  However, expandable candidate shouldn't have been a keyboard.
    //  See the note on ExpandableCandidateLayout for details.
    val listener by lazy {
        BaseKeyboard.KeyActionListener { action ->
            service.lifecycleScope.launch {
                when (action) {
                    is KeyAction.FcitxKeyAction -> fcitx.sendKey(action.act)
                    is KeyAction.CommitAction -> {
                        // TODO: this should be handled more gracefully; or CommitAction should be removed?
                        fcitx.reset()
                        service.inputConnection?.commitText(action.act, 1)
                    }
                    is KeyAction.RepeatStartAction -> service.startRepeating(action.act)
                    is KeyAction.RepeatEndAction -> service.cancelRepeating(action.act)
                    is KeyAction.QuickPhraseAction -> {
                        fcitx.reset()
                        fcitx.triggerQuickPhrase()
                    }
                    is KeyAction.UnicodeAction -> {
                        fcitx.triggerUnicode()
                    }
                    is KeyAction.LangSwitchAction -> {
                        if (fcitx.enabledIme().size < 2) {
                            AppUtil.launchMainToAddInputMethods(context)
                        } else
                            fcitx.enumerateIme()
                    }
                    is KeyAction.InputMethodSwitchAction -> inputMethodManager.showInputMethodPicker()
                    is KeyAction.CustomAction -> {
                        action.act(fcitx)
                    }
                    else -> {
                    }
                }
            }
        }
    }
}