package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment

class MainFragment : PaddingPreferenceFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
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
            setTitle(R.string.keyboard)
            setIcon(R.drawable.ic_baseline_keyboard_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_keyboardSettingsFragment)
                true
            }
        })
        androidCategory.addPreference(Preference(context).apply {
            setTitle(R.string.theme)
            setIcon(R.drawable.ic_baseline_palette_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_themeListFragment)
                true
            }
        })
        androidCategory.addPreference(Preference(context).apply {
            setTitle(R.string.clipboard)
            setIcon(R.drawable.ic_clipboard)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_clipboardSettingsFragment)
                true
            }
        })
        androidCategory.addPreference(Preference(context).apply {
            setTitle(R.string.plugins)
            setIcon(R.drawable.ic_baseline_android_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_pluginFragment)
                true
            }
        })
        androidCategory.addPreference(Preference(context).apply {
            setTitle(R.string.advanced)
            setIcon(R.drawable.ic_baseline_more_horiz_24)
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_advancedSettingsFragment)
                true
            }
        })
        preferenceScreen = screen
    }
}