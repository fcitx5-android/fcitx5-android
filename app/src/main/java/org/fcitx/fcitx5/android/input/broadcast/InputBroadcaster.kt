package org.fcitx.fcitx5.android.input.broadcast

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxEvent.InputPanelAuxEvent
import org.fcitx.fcitx5.android.core.FcitxEvent.PreeditEvent
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.UniqueComponent
import java.util.concurrent.ConcurrentLinkedQueue

class InputBroadcaster : UniqueComponent<InputBroadcaster>(), Dependent, InputBroadcastReceiver {

    private val receivers = ConcurrentLinkedQueue<InputBroadcastReceiver>()

    override fun handle(scopeEvent: ScopeEvent) {
        when (scopeEvent) {
            is ScopeEvent.DependencyArrivedEvent -> {
                if (scopeEvent.dependency is InputBroadcastReceiver && scopeEvent.dependency !is InputBroadcaster) {
                    receivers.add(scopeEvent.dependency as InputBroadcastReceiver)
                }
            }

            is ScopeEvent.DependencyLeftEvent -> {
                if (scopeEvent.dependency is InputBroadcastReceiver && scopeEvent.dependency !is InputBroadcaster) {
                    receivers.remove(scopeEvent.dependency as InputBroadcastReceiver)
                }
            }
        }
    }

    override fun onPreeditUpdate(data: PreeditEvent.Data) {
        receivers.forEach { it.onPreeditUpdate(data) }
    }

    override fun onInputPanelAuxUpdate(data: InputPanelAuxEvent.Data) {
        receivers.forEach { it.onInputPanelAuxUpdate(data) }
    }

    override fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {
        receivers.forEach { it.onStartInput(info, capFlags) }
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        receivers.forEach { it.onImeUpdate(ime) }
    }

    override fun onCandidateUpdate(data: Array<String>) {
        receivers.forEach { it.onCandidateUpdate(data) }
    }

    override fun onStatusAreaUpdate(actions: Array<Action>) {
        receivers.forEach { it.onStatusAreaUpdate(actions) }
    }

    override fun onSelectionUpdate(start: Int, end: Int) {
        receivers.forEach { it.onSelectionUpdate(start, end) }
    }

    override fun onWindowAttached(window: InputWindow) {
        receivers.forEach { it.onWindowAttached(window) }
    }

    override fun onWindowDetached(window: InputWindow) {
        receivers.forEach { it.onWindowDetached(window) }
    }

    override fun onScopeSetupFinished(scope: DynamicScope) {
        receivers.forEach { it.onScopeSetupFinished(scope) }
    }

    override fun onPunctuationUpdate(mapping: Map<String, String>) {
        receivers.forEach { it.onPunctuationUpdate(mapping) }
    }

}