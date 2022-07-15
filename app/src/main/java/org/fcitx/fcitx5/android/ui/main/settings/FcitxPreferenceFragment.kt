package org.fcitx.fcitx5.android.ui.main.settings

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.forEach
import androidx.preference.isEmpty
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.ui.main.MainViewModel

abstract class FcitxPreferenceFragment : PaddingPreferenceFragment() {
    abstract fun getPageTitle(): String
    abstract suspend fun obtainConfig(fcitx: Fcitx): RawConfig
    abstract suspend fun saveConfig(fcitx: Fcitx, newConfig: RawConfig)

    private lateinit var raw: RawConfig

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    private fun save() {
        lifecycleScope.launch {
            saveConfig(fcitx, raw["cfg"])
        }
    }

    private val onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        save()
        true
    }

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.withLoadingDialog(requireContext()) {
            raw = obtainConfig(fcitx)
            val screen =
                PreferenceScreenFactory.create(preferenceManager, parentFragmentManager, raw)
            screen.forEach {
                it.onPreferenceChangeListener = onPreferenceChangeListener
            }
            if (screen.isEmpty()) {
                screen.addPreference(Preference(requireContext()).apply {
                    setTitle(R.string.no_config_options)
                    isIconSpaceReserved = false
                })
            }
            preferenceScreen = screen
            viewModel.disableAboutButton()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getPageTitle())
    }

}