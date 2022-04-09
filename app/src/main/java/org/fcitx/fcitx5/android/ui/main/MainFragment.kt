package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.fcitx.fcitx5.android.R

class MainFragment : PreferenceFragmentCompat() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.app_name))
        viewModel.disableToolbarSaveButton()
        viewModel.enableAboutButton()
    }

    override fun onPause() {
        viewModel.disableAboutButton()
        super.onPause()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        val fcitxCategory = PreferenceCategory(context).apply {
            title = "Fcitx"
        }
        screen.addPreference(fcitxCategory)
        fcitxCategory.addPreference(Preference(context).apply {
            setTitle(R.string.global_options)
            setIcon(R.drawable.ic_baseline_tune_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_globalConfigFragment)
                true
            }
        })
        fcitxCategory.addPreference(Preference(context).apply {
            setTitle(R.string.input_methods)
            setIcon(R.drawable.ic_baseline_language_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_imListFragment)
                true
            }
        })
        fcitxCategory.addPreference(Preference(context).apply {
            setTitle(R.string.addons)
            setIcon(R.drawable.ic_baseline_extension_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_addonListFragment)
                true
            }
        })

        val androidCategory = PreferenceCategory(context).apply {
            title = "Android"
        }
        screen.addPreference(androidCategory)
        androidCategory.addPreference(Preference(context).apply {
            setTitle(R.string.behavior)
            setIcon(R.drawable.ic_baseline_keyboard_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_behaviorSettingsFragment)
                true
            }
        })
        androidCategory.addPreference(Preference(context).apply {
            setTitle(R.string.developer)
            setIcon(R.drawable.ic_baseline_developer_mode_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_developerFragment)
                true
            }
        })
        preferenceScreen = screen
    }
}