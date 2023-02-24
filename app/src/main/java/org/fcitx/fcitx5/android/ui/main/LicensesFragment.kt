package org.fcitx.fcitx5.android.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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
                .libraries
                .sortedBy {
                    if (it.tag == "native") it.uniqueId.uppercase() else it.uniqueId.lowercase()
                }
                .forEach {
                    screen.addPreference(Preference(context).apply {
                        isIconSpaceReserved = false
                        title = "${it.uniqueId}:${it.artifactVersion}"
                        summary = it.licenses.joinToString { l -> l.spdxId ?: l.name }
                        setOnPreferenceClickListener { _ ->
                            showLicenseDialog(it.uniqueId, it.licenses)
                        }
                    })
                }
            preferenceScreen = screen
        }
    }

    private fun showLicenseDialog(uniqueId: String, licenses: Set<License>): Boolean {
        when (licenses.size) {
            0 -> {}
            1 -> showLicenseContent(licenses.first())
            else -> {
                val licenseArray = licenses.toTypedArray()
                val licenseNames = licenseArray.map { it.spdxId ?: it.name }.toTypedArray()
                AlertDialog.Builder(context)
                    .setTitle(uniqueId)
                    .setItems(licenseNames) { _, idx ->
                        showLicenseContent(licenseArray[idx])
                    }
                    .setPositiveButton(android.R.string.cancel, null)
                    .show()
            }
        }
        return true
    }

    private fun showLicenseContent(license: License) {
        if (license.url?.isNotBlank() == true) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(license.url)))
        }
    }

}