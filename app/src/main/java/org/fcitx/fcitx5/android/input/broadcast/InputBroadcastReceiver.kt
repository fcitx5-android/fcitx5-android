package org.fcitx.fcitx5.android.input.broadcast

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.DynamicScope

interface InputBroadcastReceiver {

    fun onScopeSetupFinished(scope: DynamicScope) {}

    fun onEditorInfoUpdate(info: EditorInfo?) {}

    fun onPreeditUpdate(content: PreeditContent) {}

    fun onImeUpdate(ime: InputMethodEntry) {}

    fun onCandidateUpdate(data: Array<String>) {}

    fun onStatusAreaUpdate(actions: Array<Action>) {}

    fun onSelectionUpdate(start: Int, end: Int) {}

    fun onWindowAttached(window: InputWindow) {}

    fun onWindowDetached(window: InputWindow) {}

}