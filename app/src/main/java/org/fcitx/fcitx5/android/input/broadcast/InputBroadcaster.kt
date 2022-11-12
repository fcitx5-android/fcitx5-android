package org.fcitx.fcitx5.android.input.broadcast

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.FcitxEvent.InputPanelAuxEvent
import org.fcitx.fcitx5.android.core.FcitxEvent.PreeditEvent
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.utils.tStr
import org.fcitx.fcitx5.android.utils.tracer
import org.fcitx.fcitx5.android.utils.withSpan
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.UniqueComponent
import java.util.concurrent.ConcurrentLinkedQueue

class InputBroadcaster : UniqueComponent<InputBroadcaster>(), Dependent, InputBroadcastReceiver {

    private val receivers = ConcurrentLinkedQueue<InputBroadcastReceiver>()

    private val tracer by tracer(javaClass.name)

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

    override fun onPreeditUpdate(data: PreeditEvent.Data) =
        tracer.withSpan("onPreeditUpdate") {
            setAttribute("preedit.preedit", data.preedit)
            setAttribute("preedit.clientPreedit", data.clientPreedit)
            receivers.forEach {
                tracer.withSpan(it.tStr()) {
                    it.onPreeditUpdate(data)
                }
            }
        }

    override fun onInputPanelAuxUpdate(data: InputPanelAuxEvent.Data) =
        tracer.withSpan("onInputPanelAuxUpdate") {
            setAttribute("aux.up", data.auxUp)
            setAttribute("aux.down", data.auxDown)
            receivers.forEach {
                tracer.withSpan(it.tStr()) {
                    it.onInputPanelAuxUpdate(data)
                }
            }
        }

    override fun onEditorInfoUpdate(info: EditorInfo?) = tracer.withSpan("onEditorInfoUpdate") {
        setAttribute("info", info.toString())
        receivers.forEach {
            tracer.withSpan(it.tStr()) {
                it.onEditorInfoUpdate(info)
            }
        }
    }

    override fun onImeUpdate(ime: InputMethodEntry) = tracer.withSpan("onImeUpdate") {
        setAttribute("ime", ime.uniqueName)
        receivers.forEach {
            tracer.withSpan(it.tStr()) {
                it.onImeUpdate(ime)
            }
        }
    }

    override fun onCandidateUpdate(data: Array<String>) = tracer.withSpan("onCandidateUpdate") {
        setAttribute("candidates", data.joinToString())
        receivers.forEach {
            tracer.withSpan(it.tStr()) {
                it.onCandidateUpdate(data)
            }
        }
    }

    override fun onStatusAreaUpdate(actions: Array<Action>) =
        tracer.withSpan("onStatusAreaUpdate") {
            setAttribute("actions", actions.joinToString())
            receivers.forEach {
                tracer.withSpan(it.tStr()) {
                    it.onStatusAreaUpdate(actions)
                }
            }
        }

    override fun onSelectionUpdate(start: Int, end: Int) = tracer.withSpan("onSelectionUpdate") {
        setAttribute("start", start.toLong())
        setAttribute("end", end.toLong())
        receivers.forEach {
            tracer.withSpan(it.tStr()) {
                it.onSelectionUpdate(start, end)
            }
        }
    }

    override fun onWindowAttached(window: InputWindow) = tracer.withSpan("onWindowAttached") {
        setAttribute("window", window.toString())
        receivers.forEach {
            tracer.withSpan(it.tStr()) {
                it.onWindowAttached(window)
            }
        }
    }

    override fun onWindowDetached(window: InputWindow) = tracer.withSpan("onWindowDetached") {
        setAttribute("window", window.toString())
        receivers.forEach {
            tracer.withSpan(it.tStr()) {
                it.onWindowDetached(window)
            }
        }
    }

    override fun onScopeSetupFinished(scope: DynamicScope) =
        tracer.withSpan("onScopeSetupFinished") {
            receivers.forEach {
                tracer.withSpan(it.tStr()) {
                    it.onScopeSetupFinished(scope)
                }
            }
        }

    override fun onPunctuationUpdate(mapping: Map<String, String>) =
        tracer.withSpan("onPunctuationUpdate") {
            setAttribute("punctuation", mapping.toString())
            receivers.forEach {
                tracer.withSpan(it.tStr()) {
                    it.onPunctuationUpdate(mapping)
                }
            }
        }

}