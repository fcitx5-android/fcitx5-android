package me.rocka.fcitx5test.input.broadcast

import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.preedit.PreeditContent
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.UniqueComponent
import java.util.concurrent.ConcurrentLinkedQueue

class InputBroadcaster : UniqueComponent<InputBroadcaster>(), Dependent {

    private val receivers = ConcurrentLinkedQueue<InputBroadcastReceiver>()

    override fun handle(scopeEvent: ScopeEvent) {
        when (scopeEvent) {
            is ScopeEvent.DependencyArrivedEvent ->
                if (scopeEvent.dependency is InputBroadcastReceiver)
                    receivers.add(scopeEvent.dependency as InputBroadcastReceiver)
            is ScopeEvent.DependencyLeftEvent -> if (scopeEvent.dependency is InputBroadcastReceiver)
                receivers.remove(scopeEvent.dependency as InputBroadcastReceiver)
        }
    }


    fun broadcastPreeditUpdate(content: PreeditContent) {
        receivers.forEach { it.onPreeditUpdate(content) }
    }

    fun broadcastImeUpdate(ime: InputMethodEntry) {
        receivers.forEach { it.onImeUpdate(ime) }
    }

    fun broadcastCandidatesUpdate(data: Array<String>) {
        receivers.forEach { it.onCandidateUpdates(data) }
    }

}