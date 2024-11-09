/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.data.prefs

import androidx.preference.PreferenceScreen
import org.fcitx.fcitx5.android.utils.WeakHashSet

abstract class ManagedPreferenceProvider {

    fun interface OnChangeListener {
        fun onChange(key: String)
    }

    private val _managedPreferences: MutableMap<String, ManagedPreference<*>> = mutableMapOf()

    private val _managedPreferencesUi: MutableList<ManagedPreferenceUi<*>> = mutableListOf()

    val managedPreferences: Map<String, ManagedPreference<*>>
        get() = _managedPreferences

    val managedPreferencesUi: List<ManagedPreferenceUi<*>>
        get() = _managedPreferencesUi

    open fun createUi(screen: PreferenceScreen) {

    }

    private val onChangeListeners = WeakHashSet<OnChangeListener>()

    fun registerOnChangeListener(listener: OnChangeListener) {
        onChangeListeners.add(listener)
    }

    fun unregisterOnChangeListener(listener: OnChangeListener) {
        onChangeListeners.remove(listener)
    }

    fun fireChange(key: String) {
        val preference = _managedPreferences[key] ?: return
        onChangeListeners.forEach { it.onChange(key) }
        preference.fireChange()
    }

    fun ManagedPreferenceUi<*>.registerUi() {
        _managedPreferencesUi.add(this)
    }

    fun ManagedPreference<*>.register() {
        _managedPreferences[key] = this
    }

}