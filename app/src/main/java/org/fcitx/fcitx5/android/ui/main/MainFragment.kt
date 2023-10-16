package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceCategory
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.utils.addCategory
import org.fcitx.fcitx5.android.utils.addPreference

class MainFragment : PaddingPreferenceFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onStart() {
        super.onStart()
        viewModel.enableAboutButton()
    }

    override fun onStop() {
        viewModel.disableAboutButton()
        super.onStop()
    }

    private fun PreferenceCategory.addDestinationPreference(
        @StringRes title: Int,
        @DrawableRes icon: Int,
        @IdRes destination: Int
    ) {
        addPreference(title, icon = icon) {
            findNavController().navigate(destination)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addCategory("Fcitx") {
                addDestinationPreference(
                    R.string.global_options,
                    R.drawable.ic_baseline_tune_24,
                    R.id.action_mainFragment_to_globalConfigFragment
                )
                addDestinationPreference(
                    R.string.input_methods,
                    R.drawable.ic_baseline_language_24,
                    R.id.action_mainFragment_to_imListFragment
                )
                addDestinationPreference(
                    R.string.addons,
                    R.drawable.ic_baseline_extension_24,
                    R.id.action_mainFragment_to_addonListFragment
                )
            }
            addCategory("Android") {
                addDestinationPreference(
                    R.string.keyboard,
                    R.drawable.ic_baseline_keyboard_24,
                    R.id.action_mainFragment_to_keyboardSettingsFragment
                )
                addDestinationPreference(
                    R.string.theme,
                    R.drawable.ic_baseline_palette_24,
                    R.id.action_mainFragment_to_themeFragment
                )
                addDestinationPreference(
                    R.string.clipboard,
                    R.drawable.ic_clipboard,
                    R.id.action_mainFragment_to_clipboardSettingsFragment
                )
                addDestinationPreference(
                    R.string.plugins,
                    R.drawable.ic_baseline_android_24,
                    R.id.action_mainFragment_to_pluginFragment
                )
                addDestinationPreference(
                    R.string.advanced,
                    R.drawable.ic_baseline_more_horiz_24,
                    R.id.action_mainFragment_to_advancedSettingsFragment
                )
            }
        }
    }
}