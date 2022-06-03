package org.fcitx.fcitx5.android.data.prefs

import androidx.preference.PreferenceScreen

interface ManagedPreferenceProvider {

    val managedPreferences: MutableMap<String, ManagedPreference<*, *>>

    fun createUi(screen: PreferenceScreen) {

    }

    fun ManagedPreference<*, *>.register() {
        managedPreferences[key] = this
    }

}