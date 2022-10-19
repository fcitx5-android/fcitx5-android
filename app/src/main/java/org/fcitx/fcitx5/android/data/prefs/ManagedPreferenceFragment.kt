package org.fcitx.fcitx5.android.data.prefs

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
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

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        evaluator.evaluateVisibility()
        preferenceScreen =
            preferenceManager.createPreferenceScreen(preferenceManager.context).also { screen ->
                preferenceProvider.createUi(screen)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        evaluator.destroy()
    }
}