/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import kotlinx.coroutines.flow.SharedFlow

/**
 * API of fcitx that hides lifecycle stuffs from [Fcitx]
 *
 * Functions can be safely used in any coroutine,
 * as the underlying operation is always dispatched in fcitx thread.
 */
interface FcitxAPI {


    enum class AddonDep {
        Required,
        Optional
    }

    /**
     * Subscribe this flow to receive event sent from fcitx
     */
    val eventFlow: SharedFlow<FcitxEvent<*>>

    val isReady: Boolean

    val inputMethodEntryCached: InputMethodEntry

    val statusAreaActionsCached: Array<Action>

    val clientPreeditCached: FormattedText

    val inputPanelCached: FcitxEvent.InputPanelEvent.Data

    fun getAddonReverseDependencies(addon: String): List<Pair<String, AddonDep>>

    fun translate(str: String, domain: String = "fcitx5"): String

    suspend fun save()

    suspend fun reloadConfig()

    suspend fun sendKey(key: String, states: UInt = 0u, up: Boolean = false, timestamp: Int = -1)

    suspend fun sendKey(c: Char, states: UInt = 0u, up: Boolean = false, timestamp: Int = -1)

    suspend fun sendKey(sym: Int, states: UInt = 0u, up: Boolean = false, timestamp: Int = -1)

    suspend fun sendKey(sym: KeySym, states: KeyStates, up: Boolean = false, timestamp: Int = -1)

    suspend fun select(idx: Int): Boolean
    suspend fun isEmpty(): Boolean
    suspend fun reset()
    suspend fun moveCursor(position: Int)

    suspend fun availableIme(): Array<InputMethodEntry>
    suspend fun enabledIme(): Array<InputMethodEntry>

    suspend fun setEnabledIme(array: Array<String>)

    suspend fun toggleIme()
    suspend fun activateIme(ime: String)
    suspend fun enumerateIme(forward: Boolean = true)

    suspend fun currentIme(): InputMethodEntry

    suspend fun getGlobalConfig(): RawConfig

    suspend fun setGlobalConfig(config: RawConfig)

    suspend fun getAddonConfig(addon: String): RawConfig

    suspend fun setAddonConfig(addon: String, config: RawConfig)

    suspend fun getAddonSubConfig(addon: String, path: String): RawConfig

    suspend fun setAddonSubConfig(addon: String, path: String, config: RawConfig = RawConfig())

    suspend fun getImConfig(key: String): RawConfig

    suspend fun setImConfig(key: String, config: RawConfig)

    suspend fun addons(): Array<AddonInfo>
    suspend fun setAddonState(name: Array<String>, state: BooleanArray)

    suspend fun triggerQuickPhrase()
    suspend fun triggerUnicode()

    suspend fun focus(focus: Boolean = true)
    suspend fun activate(uid: Int, pkgName: String)
    suspend fun deactivate(uid: Int)
    suspend fun setCapFlags(flags: CapabilityFlags)

    suspend fun statusArea(): Array<Action>

    suspend fun activateAction(id: Int)

    suspend fun getCandidates(offset: Int, limit: Int): Array<String>

}