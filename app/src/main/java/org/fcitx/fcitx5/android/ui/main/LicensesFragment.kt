package org.fcitx.fcitx5.android.ui.main

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.License
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
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
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)
            val jsonString = resources.openRawResource(R.raw.aboutlibraries)
                .bufferedReader()
                .use { it.readText() }
            Libs.Builder()
                .withJson(jsonString)
                .build()
                .libraries.forEach {
                    screen.addPreference(Preference(context).apply {
                        isIconSpaceReserved = false
                        title = "${it.uniqueId}:${it.artifactVersion}"
                        val license = it.licenses.firstOrNull() ?: return@forEach
                        summary = license.spdxId ?: license.name
                        setOnPreferenceClickListener {
                            showLicenseDialog(license)
                        }
                    })
                }
            preferenceScreen = screen
        }
    }

    private fun showLicenseDialog(license: License): Boolean {
        AlertDialog.Builder(context)
            .setTitle(license.name)
            .setMessage(license.licenseContent)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        return true
    }

}