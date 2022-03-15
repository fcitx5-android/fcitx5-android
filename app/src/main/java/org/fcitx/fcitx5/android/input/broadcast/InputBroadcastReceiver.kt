package org.fcitx.fcitx5.android.input.broadcast

import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.DynamicScope

interface InputBroadcastReceiver {

    fun onScopeSetupFinished(scope: DynamicScope) {}

    fun onPreeditUpdate(content: PreeditContent) {}

    fun onImeUpdate(ime: InputMethodEntry) {}

    fun onCandidateUpdates(data: Array<String>) {}

    fun onWindowAttached(window: InputWindow) {}

    fun onWindowDetached(window: InputWindow) {}

}