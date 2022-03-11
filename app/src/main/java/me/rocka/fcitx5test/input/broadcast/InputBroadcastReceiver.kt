package me.rocka.fcitx5test.input.broadcast

import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.input.wm.InputWindow
import org.mechdancer.dependency.DynamicScope

interface InputBroadcastReceiver {

    fun onScopeSetupFinished(scope: DynamicScope) {}

    fun onPreeditUpdate(content: PreeditContent) {}

    fun onImeUpdate(ime: InputMethodEntry) {}

    fun onCandidateUpdates(data: Array<String>) {}

    fun onWindowAttached(window: InputWindow) {}

    fun onWindowDetached(window: InputWindow) {}

}