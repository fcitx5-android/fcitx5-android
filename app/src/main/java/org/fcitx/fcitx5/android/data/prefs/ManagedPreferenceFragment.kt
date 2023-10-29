/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import android.os.Build
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment

abstract class ManagedPreferenceFragment(private val preferenceProvider: ManagedPreferenceProvider) :
    PaddingPreferenceFragment() {

    private val evaluator = ManagedPreferenceVisibilityEvaluator(preferenceProvider) {
        lifecycleScope.launch {
            it.forEach { (key, enable) ->
                findPreference<Preference>(key)?.isEnabled = enable
            }
        }
    }

    open fun onPreferenceUiCreated(screen: PreferenceScreen) {}

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        evaluator.evaluateVisibility()
        preferenceScreen =
            preferenceManager.createPreferenceScreen(preferenceManager.context).also { screen ->
                preferenceProvider.createUi(screen)
                onPreferenceUiCreated(screen)
            }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AppPrefs.getInstance().syncToDeviceEncryptedStorage()
        }
        super.onStop()
    }

    override fun onDestroy() {
        evaluator.destroy()
        super.onDestroy()
    }
}