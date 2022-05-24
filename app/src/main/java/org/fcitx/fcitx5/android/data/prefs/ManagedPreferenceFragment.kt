package org.fcitx.fcitx5.android.data.prefs

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment

abstract class ManagedPreferenceFragment(private val preferenceProvider: ManagedPreferenceProvider) :
    PaddingPreferenceFragment() {

    private val evaluator =
        ManagedPreferenceVisibilityEvaluator(preferenceProvider.managedPreferences) {
            lifecycleScope.launch {
                it.forEach { (key, enable) ->
                    findPreference<Preference>(key)?.isEnabled = enable
                }
            }
        }

    private fun createUi() {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceProvider.createUi(screen)
        preferenceScreen = screen
    }

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        evaluator.evaluateVisibility()
        createUi()
    }
}