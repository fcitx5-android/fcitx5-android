/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import androidx.annotation.Keep

class ManagedPreferenceVisibilityEvaluator(
    private val provider: ManagedPreferenceProvider,
    private val onVisibilityChanged: (Map<String, Boolean>) -> Unit
) {

    private val visibility = mutableMapOf<String, Boolean>()

    // it would be better to declare the dependency relationship, rather than reevaluating on each value changed
    @Keep
    private val onValueChangeListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        evaluateVisibility()
    }

    init {
        provider.managedPreferences.forEach { (_, pref) ->
            pref.registerOnChangeListener(onValueChangeListener)
        }
    }

    fun evaluateVisibility() {
        val changed = mutableMapOf<String, Boolean>()
        provider.managedPreferencesUi.forEach { ui ->
            val old = visibility[ui.key]
            val new = ui.isEnabled()
            if (old != null && old != new) {
                changed[ui.key] = new
            }
            visibility[ui.key] = new
        }
        if (changed.isNotEmpty())
            onVisibilityChanged(changed)
    }

    fun destroy() {
        provider.managedPreferences.forEach { (_, pref) ->
            pref.unregisterOnChangeListener(onValueChangeListener)
        }
    }

}