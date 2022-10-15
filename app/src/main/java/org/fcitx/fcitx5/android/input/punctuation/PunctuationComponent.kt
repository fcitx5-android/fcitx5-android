package org.fcitx.fcitx5.android.input.punctuation

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.dependency.fcitx
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class PunctuationComponent : InputBroadcastReceiver,
    UniqueComponent<PunctuationComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val fcitx by manager.fcitx()
    private val service by manager.inputMethodService()
    private val broadcaster: InputBroadcaster by manager.must()

    private var mapping: Map<String, String> = mapOf()

    var enabled: Boolean = false
        private set

    fun transform(p: String) = mapping.getOrDefault(p, p)

    private var updateJob: Job? = null

    private fun updateMapping() {
        updateJob = service.lifecycleScope.launch {
            updateJob?.cancel()
            mapping = if (enabled) {
                fcitx.runOnReady {
                    PunctuationManager.load(this, inputMethodEntryCached.languageCode)
                        .associate { it.key to it.mapping }
                }
            } else mapOf()
            broadcaster.onPunctuationUpdate(mapping)
            updateJob = null
        }
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
