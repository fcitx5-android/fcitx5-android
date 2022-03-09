package me.rocka.fcitx5test.input

import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.input.preedit.PreeditContent
import org.mechdancer.dependency.Component
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import java.util.concurrent.ConcurrentLinkedQueue

class InputBroadcaster : UniqueComponent<InputBroadcaster>(), Dependent {

    private val receivers = ConcurrentLinkedQueue<InputBroadcastReceiver>()

    override fun handle(dependency: Component): Boolean {
        if (dependency is InputBroadcastReceiver)
            receivers.add(dependency)
        return false
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