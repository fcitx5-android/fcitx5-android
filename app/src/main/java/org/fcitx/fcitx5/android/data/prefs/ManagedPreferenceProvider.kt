package org.fcitx.fcitx5.android.data.prefs

import androidx.preference.PreferenceScreen

interface ManagedPreferenceProvider {

    val managedPreferences: Map<String, ManagedPreference<*, *>>

    fun createUi(screen: PreferenceScreen) {

    }

}