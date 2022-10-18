package org.fcitx.fcitx5.android.data.prefs

import androidx.preference.PreferenceScreen

abstract class ManagedPreferenceProvider {

    private val _managedPreferences: MutableMap<String, ManagedPreference<*>> = mutableMapOf()

    private val _managedPreferencesUi: MutableList<ManagedPreferenceUi<*>> = mutableListOf()

    val managedPreferences: Map<String, ManagedPreference<*>>
        get() = _managedPreferences

    val managedPreferencesUi: List<ManagedPreferenceUi<*>>
        get() = _managedPreferencesUi

    open fun createUi(screen: PreferenceScreen) {

    }

    fun ManagedPreferenceUi<*>.registerUi() {
        _managedPreferencesUi.add(this)
    }

    fun ManagedPreference<*>.register() {
        _managedPreferences[key] = this
    }

}