package org.fcitx.fcitx5.android.input.punctuation

import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler

class PunctuationComponent :
    UniqueComponent<PunctuationComponent>(), InputBroadcastReceiver,
    Dependent, ManagedHandler by managedHandler() {

    private val fcitx by manager.fcitx()

    private var mapping: Map<String, String> = mapOf()

    var enabled: Boolean = false
        private set

    fun transform(p: String) = mapping.getOrDefault(p, p)

    private fun updateMapping() {
        mapping = if (enabled) {
            runBlocking {
                PunctuationManager.load(fcitx, fcitx.inputMethodEntryCached.languageCode)
                    .associate { it.key to it.mapping }
            }
        } else mapOf()
    }

    override fun onStatusAreaUpdate(actions: Array<Action>) {
        enabled = actions.any {
            // TODO A better way to check if punctuation mapping is enabled
            it.name == "punctuation" && it.icon == "fcitx-punc-active"
        }
        updateMapping()
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        updateMapping()
    }
}
