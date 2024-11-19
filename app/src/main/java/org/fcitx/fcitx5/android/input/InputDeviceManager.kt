/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.candidates.floating.FloatingCandidatesMode
import org.fcitx.fcitx5.android.utils.monitorCursorAnchor

class InputDeviceManager(private val onChange: (Boolean) -> Unit) {

    private var inputView: InputView? = null
    private var candidatesView: CandidatesView? = null

    private fun setupInputViewEvents(isVirtual: Boolean) {
        inputView?.handleEvents = isVirtual
        inputView?.visibility = if (isVirtual) View.VISIBLE else View.GONE
    }

    private fun setupCandidatesViewEvents(isVirtual: Boolean) {
        candidatesView?.handleEvents = !isVirtual
        // hide CandidatesView when entering virtual keyboard mode,
        // but preserve the visibility when entering physical keyboard mode (in case it's empty)
        if (isVirtual) {
            candidatesView?.visibility = View.GONE
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

    /**
     * @return should use virtual keyboard
     */
    fun evaluateOnStartInputView(info: EditorInfo, service: FcitxInputMethodService): Boolean {
        startedInputView = true
        isNullInputType = info.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL
        val useVirtualKeyboard = when (candidatesViewMode) {
            FloatingCandidatesMode.SystemDefault -> service.superEvaluateInputViewShown()
            FloatingCandidatesMode.InputDevice -> isVirtualKeyboard
            FloatingCandidatesMode.Disabled -> true
        }
        applyMode(service, useVirtualKeyboard)
        return useVirtualKeyboard
    }

    /**
     * @return should force show input views
     */
    fun evaluateOnKeyDown(e: KeyEvent, service: FcitxInputMethodService): Boolean {
        if (startedInputView) {
            // filter out back/home/volume buttons
            if (e.isPrintingKey) {
                // evaluate virtual keyboard visibility when pressing physical keyboard while InputView visible
                evaluateOnKeyDownInner(service)
            }
            // no need to force show InputView since it's already visible
            return false
        } else {
            // force show InputView when focusing on text input (likely inputType is not TYPE_NULL)
            // and pressing any digit/letter/punctuation key on physical keyboard
            val showInputView = !isNullInputType && e.isPrintingKey
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

    fun onFinishInputView() {
        startedInputView = false
    }
}
