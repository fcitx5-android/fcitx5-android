package org.fcitx.fcitx5.android.ui.main.settings.behavior

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment
import org.fcitx.fcitx5.android.ui.main.MainViewModel

class BehaviorSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance()) {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        viewModel.disableToolbarSaveButton()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.behavior))
    }
}
