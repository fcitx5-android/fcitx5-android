package org.fcitx.fcitx5.android.input.keyboard

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.daemon.launchOnFcitxReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dialog.AddMoreInputMethodsPrompt
import org.fcitx.fcitx5.android.input.dialog.InputMethodSwitcherDialog
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.*
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.*
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
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
    private val windowManager: InputWindowManager by manager.must()

    private var lastPickerType by AppPrefs.getInstance().internal.lastPickerType

    private var backspaceSwipeState = Stopped

    private suspend fun FcitxAPI.commitAndReset() {
        if (preeditCached.run { preedit.isEmpty() && clientPreedit.isEmpty() }) {
            // preedit is empty, there can be prediction candidates
            reset()
        } else if (inputMethodEntryCached.uniqueName.let { it == "keyboard-us" || it == "unikey" }) {
            // androidkeyboard clears composing on reset, but we want to commit it as-is
            service.inputConnection?.finishComposingText()
            reset()
        } else {
            if (!select(0)) reset()
        }
    }

    val listener by lazy {
        KeyActionListener { action, _ ->
            service.lifecycleScope.launchOnFcitxReady(fcitx) {
                when (action) {
                    is FcitxKeyAction -> it.sendKey(action.act, KeyState.Virtual.state)
                    is SymAction -> it.sendKey(action.sym, action.states)
                    is CommitAction -> {
                        it.commitAndReset()
                        service.commitText(action.text)
                    }
                    is QuickPhraseAction -> {
                        it.commitAndReset()
                        it.triggerQuickPhrase()
                    }
                    is UnicodeAction -> {
                        it.commitAndReset()
                        it.triggerUnicode()
                    }
                    is LangSwitchAction -> {
                        if (it.enabledIme().size < 2) {
                            inputView.showDialog(AddMoreInputMethodsPrompt.build(context))
                        } else if (action.enumerate) {
                            it.enumerateIme()
                        } else {
                            it.toggleIme()
                        }
                    }
                    is InputMethodSwitchAction -> {
                        inputView.showDialog(
                            InputMethodSwitcherDialog.build(it, service, context)
                        )
                    }
                    is MoveSelectionAction -> when (backspaceSwipeState) {
                        Stopped -> backspaceSwipeState = it.preeditCached.let { p ->
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
                            Selection -> service.deleteSelection()
                            Reset -> if (action.totalCnt < 0) { // swipe left
                                it.reset()
                            }
                        }
                        backspaceSwipeState = Stopped
                    }
                    is PickerSwitchAction -> {
                        // update lastSymbolType only when specified explicitly
                        val key = action.key?.also { k -> lastPickerType = k.name }
                            ?: runCatching { PickerWindow.Key.valueOf(lastPickerType) }.getOrNull()
                            ?: PickerWindow.Key.Emoji
                        ContextCompat.getMainExecutor(service).execute {
                            windowManager.attachWindow(key)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }
}
