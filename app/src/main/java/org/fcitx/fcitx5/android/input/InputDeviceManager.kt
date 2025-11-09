/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.candidates.floating.FloatingCandidatesMode
import org.fcitx.fcitx5.android.utils.isTypeNull
import org.fcitx.fcitx5.android.utils.monitorCursorAnchor

class InputDeviceManager(private val onChange: (Boolean) -> Unit) {

    private var inputView: InputView? = null
    private var candidatesView: CandidatesView? = null

    private fun setupInputViewEvents(isVirtual: Boolean) {
        val iv = inputView ?: return
        iv.handleEvents = isVirtual
        if (isVirtual) {
            iv.visibility = View.VISIBLE
            iv.refreshWithCachedEvents()
        } else {
            iv.visibility = View.GONE
        }
    }

    private fun setupCandidatesViewEvents(isVirtual: Boolean) {
        val cv = candidatesView ?: return
        cv.handleEvents = !isVirtual
        // hide CandidatesView when entering virtual keyboard mode,
        // but preserve the visibility when entering physical keyboard mode (in case it's empty)
        if (isVirtual) {
            cv.visibility = View.GONE
        } else {
            cv.refreshWithCachedEvents()
        }
    }

    private fun setupViewEvents(isVirtual: Boolean) {
        setupInputViewEvents(isVirtual)
        setupCandidatesViewEvents(isVirtual)
    }

    var isVirtualKeyboard = true
        private set(value) {
            field = value
            setupViewEvents(value)
        }

    fun setInputView(inputView: InputView) {
        this.inputView = inputView
        setupInputViewEvents(this.isVirtualKeyboard)
    }

    fun setCandidatesView(candidatesView: CandidatesView) {
        this.candidatesView = candidatesView
        setupCandidatesViewEvents(this.isVirtualKeyboard)
    }

    private fun applyMode(service: FcitxInputMethodService, useVirtualKeyboard: Boolean) {
        if (useVirtualKeyboard == isVirtualKeyboard) {
            return
        }
        // monitor CursorAnchorInfo when switching to CandidatesView
        service.currentInputConnection.monitorCursorAnchor(!useVirtualKeyboard)
        service.postFcitxJob {
            setCandidatePagingMode(if (useVirtualKeyboard) 0 else 1)
        }
        isVirtualKeyboard = useVirtualKeyboard
        onChange(isVirtualKeyboard)
    }

    private var startedInputView = false
    private var isNullInputType = true

    private var candidatesViewMode by AppPrefs.getInstance().candidates.mode

    fun notifyOnStartInput(attribute: EditorInfo) {
        isNullInputType = attribute.isTypeNull()
    }

    /**
     * @return should use virtual keyboard
     */
    fun evaluateOnStartInputView(info: EditorInfo, service: FcitxInputMethodService): Boolean {
        startedInputView = true
        isNullInputType = info.isTypeNull()
        val useVirtualKeyboard = when (candidatesViewMode) {
            FloatingCandidatesMode.SystemDefault -> service.superEvaluateInputViewShown()
            FloatingCandidatesMode.InputDevice -> isVirtualKeyboard
            FloatingCandidatesMode.Disabled -> true
        }
        applyMode(service, useVirtualKeyboard)
        return useVirtualKeyboard
    }

    /**
     * @return should force show input views on hardware key down
     */
    fun evaluateOnKeyDown(e: KeyEvent, service: FcitxInputMethodService): Boolean {
        if (startedInputView) {
            // filter out back/home/volume buttons and combination keys
            if (e.unicodeChar != 0) {
                // evaluate virtual keyboard visibility when pressing physical keyboard while InputView visible
                evaluateOnKeyDownInner(service)
            }
            // no need to force show InputView since it's already visible
            return false
        } else {
            // force show InputView when focusing on text input (likely inputType is not TYPE_NULL)
            // and pressing any digit/letter/punctuation key on physical keyboard
            val showInputView = !isNullInputType && e.unicodeChar != 0
            if (showInputView) {
                evaluateOnKeyDownInner(service)
            }
            return showInputView
        }
    }

    private fun evaluateOnKeyDownInner(service: FcitxInputMethodService) {
        val useVirtualKeyboard = when (candidatesViewMode) {
            FloatingCandidatesMode.SystemDefault -> service.superEvaluateInputViewShown()
            FloatingCandidatesMode.InputDevice -> false
            FloatingCandidatesMode.Disabled -> true
        }
        applyMode(service, useVirtualKeyboard)
    }

    fun evaluateOnViewClicked(service: FcitxInputMethodService) {
        if (!startedInputView) return
        val useVirtualKeyboard = when (candidatesViewMode) {
            FloatingCandidatesMode.SystemDefault -> service.superEvaluateInputViewShown()
            else -> true
        }
        applyMode(service, useVirtualKeyboard)
    }

    fun evaluateOnUpdateEditorToolType(toolType: Int, service: FcitxInputMethodService) {
        if (!startedInputView) return
        val useVirtualKeyboard = when (candidatesViewMode) {
            FloatingCandidatesMode.SystemDefault -> service.superEvaluateInputViewShown()
            FloatingCandidatesMode.InputDevice ->
                // switch to virtual keyboard on touch screen events, otherwise preserve current mode
                if (toolType == MotionEvent.TOOL_TYPE_FINGER || toolType == MotionEvent.TOOL_TYPE_STYLUS) true else isVirtualKeyboard
            FloatingCandidatesMode.Disabled -> true
        }
        applyMode(service, useVirtualKeyboard)
    }

    /**
     * @return should force show inputView for [CandidatesView] when input method switched by user
     */
    fun evaluateOnInputMethodChange(): Boolean {
        return !isVirtualKeyboard && !startedInputView
    }

    fun onFinishInputView() {
        startedInputView = false
    }
}
