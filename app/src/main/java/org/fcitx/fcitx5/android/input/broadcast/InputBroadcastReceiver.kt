package org.fcitx.fcitx5.android.input.broadcast

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxEvent.InputPanelEvent
import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.DynamicScope

interface InputBroadcastReceiver {

    fun onScopeSetupFinished(scope: DynamicScope) {}

    fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {}

    fun onClientPreeditUpdate(data: FormattedText) {}

    fun onInputPanelUpdate(data: InputPanelEvent.Data) {}

    fun onImeUpdate(ime: InputMethodEntry) {}

    fun onCandidateUpdate(data: Array<String>) {}

    fun onStatusAreaUpdate(actions: Array<Action>) {}

    fun onSelectionUpdate(start: Int, end: Int) {}

    fun onWindowAttached(window: InputWindow) {}

    fun onWindowDetached(window: InputWindow) {}

    fun onPunctuationUpdate(mapping: Map<String, String>) {}

}