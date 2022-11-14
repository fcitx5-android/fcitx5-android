package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.DataManager
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.utils.AppUtil

class DeveloperFragment : PaddingPreferenceFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        screen.addPreference(Preference(context).apply {
            setTitle(R.string.real_time_logs)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                AppUtil.launchLog(context)
                true
            }
        })
        screen.addPreference(SwitchPreference(context).apply {
            key = AppPrefs.getInstance().internal.verboseLog.key
            setTitle(R.string.verbose_log)
            setSummary(R.string.verbose_log_summary)
            setDefaultValue(false)
            isIconSpaceReserved = false
            isSingleLineTitle = false
        })
        screen.addPreference(SwitchPreference(context).apply {
            key = AppPrefs.getInstance().internal.editorInfoInspector.key
            setTitle(R.string.editor_info_inspector)
            setDefaultValue(false)
            isIconSpaceReserved = false
            isSingleLineTitle = false
        })

        screen.addPreference(Preference(context).apply {
            setTitle(R.string.delete_and_sync_data)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                AlertDialog.Builder(context)
                    .setTitle(R.string.delete_and_sync_data)
                    .setMessage(R.string.delete_and_sync_data_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            DataManager.deleteAndSync()
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    getString(R.string.synced),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
        })
        screen.addPreference(Preference(context).apply {
            setTitle(R.string.clear_clb_db)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                lifecycleScope.launch {
                    ClipboardManager.nukeTable()
                    Toast.makeText(context, getString(R.string.done), Toast.LENGTH_SHORT).show()
                }
                true
            }
        })
        preferenceScreen = screen
    }


    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getString(R.string.developer))
        viewModel.disableToolbarSaveButton()
    }
}