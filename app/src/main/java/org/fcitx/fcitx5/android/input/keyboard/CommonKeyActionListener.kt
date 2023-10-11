package org.fcitx.fcitx5.android.input.keyboard

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dialog.AddMoreInputMethodsPrompt
import org.fcitx.fcitx5.android.input.dialog.InputMethodPickerDialog
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.Reset
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.Selection
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.Stopped
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.CommitAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.DeleteSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.FcitxKeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.LangSwitchAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.MoveSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.PickerSwitchAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.QuickPhraseAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.ShowInputMethodPickerAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SpaceLongPressAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SymAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.UnicodeAction
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
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
    private val spaceKeyLongPressBehavior by AppPrefs.getInstance().keyboard.spaceKeyLongPressBehavior

    private var backspaceSwipeState = Stopped

    private suspend fun FcitxAPI.commitAndReset() {
        if (clientPreeditCached.isEmpty() && inputPanelCached.preedit.isEmpty()) {
            // preedit is empty, there can be prediction candidates
            reset()
        } else if (inputMethodEntryCached.uniqueName.let { it == "keyboard-us" || it == "unikey" }) {
            // androidkeyboard clears composing on reset, but we want to commit it as-is
            service.finishComposing()
            reset()
        } else {
            if (!select(0)) reset()
        }
    }

    private fun showInputMethodPicker() {
        fcitx.launchOnReady {
            service.lifecycleScope.launch {
                inputView.showDialog(InputMethodPickerDialog.build(it, service, context))
            }
        }
    }

    val listener by lazy {
        KeyActionListener { action, _ ->
            when (action) {
                is FcitxKeyAction -> fcitx.launchOnReady {
                    it.sendKey(action.act, KeyState.Virtual.state)
                }
                is SymAction -> fcitx.launchOnReady {
                    it.sendKey(action.sym, action.states)
                }
                is CommitAction -> fcitx.launchOnReady {
                    it.commitAndReset()
                    service.lifecycleScope.launch { service.commitText(action.text) }
                }
                is QuickPhraseAction -> fcitx.launchOnReady {
                    it.commitAndReset()
                    it.triggerQuickPhrase()
                }
                is UnicodeAction -> fcitx.launchOnReady {
                    it.commitAndReset()
                    it.triggerUnicode()
                }
                is LangSwitchAction -> fcitx.launchOnReady {
                    if (it.enabledIme().size < 2) {
                        service.lifecycleScope.launch {
                            inputView.showDialog(AddMoreInputMethodsPrompt.build(context))
                        }
                    } else {
                        it.enumerateIme()
                    }
                }
                is ShowInputMethodPickerAction -> showInputMethodPicker()
                is MoveSelectionAction -> {
                    when (backspaceSwipeState) {
                        Stopped -> {
                            val preeditEmpty = fcitx.runImmediately {
                                clientPreeditCached.isEmpty() && inputPanelCached.preedit.isEmpty()
                            }
                            backspaceSwipeState = if (preeditEmpty) {
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
                }
                is DeleteSelectionAction -> {
                    when (backspaceSwipeState) {
                        Stopped -> {}
                        Selection -> service.deleteSelection()
                        Reset -> if (action.totalCnt < 0) { // swipe left
                            fcitx.launchOnReady { it.reset() }
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
                is SpaceLongPressAction -> {
                    when (spaceKeyLongPressBehavior) {
                        SpaceLongPressBehavior.None -> {}
                        SpaceLongPressBehavior.Enumerate -> fcitx.launchOnReady {
                            it.enumerateIme()
                        }
                        SpaceLongPressBehavior.ToggleActivate -> fcitx.launchOnReady {
                            it.toggleIme()
                        }
                        SpaceLongPressBehavior.ShowPicker -> showInputMethodPicker()
                    }
                }
                else -> {}
            }
        }
    }
}
