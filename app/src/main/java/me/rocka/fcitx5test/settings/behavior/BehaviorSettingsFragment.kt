package me.rocka.fcitx5test.settings.behavior

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import me.rocka.fcitx5test.MainViewModel
import me.rocka.fcitx5test.R

class BehaviorSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        viewModel.setToolbarTitle(requireContext().getString(R.string.behavior))
        viewModel.disableToolbarSaveButton()
        setPreferencesFromResource(R.xml.preference, rootKey)
    }
}
