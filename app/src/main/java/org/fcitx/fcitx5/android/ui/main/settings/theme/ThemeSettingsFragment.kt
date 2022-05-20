package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.PaddingPreferenceFragment

class ThemeSettingsFragment: PaddingPreferenceFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        viewModel.disableToolbarSaveButton()
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        ThemeManager.prefs.createUi(screen)
        preferenceScreen = screen
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.theme))
    }
}