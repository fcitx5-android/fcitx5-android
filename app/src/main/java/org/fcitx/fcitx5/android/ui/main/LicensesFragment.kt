package org.fcitx.fcitx5.android.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.Licenses
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment

class LicensesFragment : PaddingPreferenceFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getString(R.string.open_source_licenses))
        viewModel.disableToolbarSaveButton()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.launch {
            Licenses.getAll().onSuccess { licenses ->
                val context = preferenceManager.context
                val screen = preferenceManager.createPreferenceScreen(context)
                licenses.forEach { license ->
                    screen.addPreference(Preference(context).apply {
                        isIconSpaceReserved = false
                        title = license.libraryName
                        summary = license.artifactId.group
                        setOnPreferenceClickListener {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(license.licenseUrl)))
                            true
                        }
                    })
                }
                preferenceScreen = screen
            }
        }
    }

}