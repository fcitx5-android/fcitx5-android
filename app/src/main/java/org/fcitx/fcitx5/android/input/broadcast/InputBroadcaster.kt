package org.fcitx.fcitx5.android.input.broadcast

import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
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

    override fun onPreeditUpdate(content: PreeditContent) {
        receivers.forEach { it.onPreeditUpdate(content) }
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        receivers.forEach { it.onImeUpdate(ime) }
    }

    override fun onCandidateUpdates(data: Array<String>) {
        receivers.forEach { it.onCandidateUpdates(data) }
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

}