/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.broadcast

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxEvent.CandidateListEvent
import org.fcitx.fcitx5.android.core.FcitxEvent.InputPanelEvent
import org.fcitx.fcitx5.android.core.FormattedText
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

    override fun onClientPreeditUpdate(data: FormattedText) {
        receivers.forEach { it.onClientPreeditUpdate(data) }
    }

    override fun onInputPanelUpdate(data: InputPanelEvent.Data) {
        receivers.forEach { it.onInputPanelUpdate(data) }
    }

    override fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {
        receivers.forEach { it.onStartInput(info, capFlags) }
    }

    override fun onImeUpdate(ime: InputMethodEntry) {
        receivers.forEach { it.onImeUpdate(ime) }
    }

    override fun onCandidateUpdate(data: CandidateListEvent.Data) {
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

    override fun onPreeditEmptyStateUpdate(empty: Boolean) {
        receivers.forEach { it.onPreeditEmptyStateUpdate(empty) }
    }

    override fun onReturnKeyDrawableUpdate(resourceId: Int) {
        receivers.forEach { it.onReturnKeyDrawableUpdate(resourceId) }
    }

}