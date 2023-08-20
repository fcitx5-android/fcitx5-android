package org.fcitx.fcitx5.android.ui.main.settings.behavior

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.UserDataManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.addPreference
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.iso8601UTCDateTime
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.toast

class AdvancedSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().advanced) {

    private val viewModel: MainViewModel by activityViewModels()

    private var exportTimestamp = System.currentTimeMillis()

    private lateinit var exportLauncher: ActivityResultLauncher<String>

    private lateinit var importLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                val cr = ctx.contentResolver
                lifecycleScope.withLoadingDialog(ctx) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        val name = cr.queryFileName(uri) ?: return@withContext
                        if (!name.endsWith(".zip")) {
                            importErrorDialog(
                                getString(R.string.exception_user_data_filename, name)
                            )
                            return@withContext
                        }
                        try {
                            val inputStream = cr.openInputStream(uri)!!
                            val metadata = UserDataManager.import(inputStream).getOrThrow()
                            withContext(Dispatchers.Main) {
                                AlertDialog.Builder(ctx)
                                    .setTitle(R.string.import_user_data)
                                    .setMessage(
                                        getString(
                                            R.string.user_data_imported,
                                            iso8601UTCDateTime(metadata.exportTime)
                                        )
                                    )
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        AppUtil.exit()
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setIconAttribute(android.R.attr.alertDialogIcon)
                                    .show()
                            }
                        } catch (e: Exception) {
                            importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
                        }
                    }
                }
            }
        exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                lifecycleScope.withLoadingDialog(requireContext()) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        try {
                            val outputStream = ctx.contentResolver.openOutputStream(uri)!!
                            UserDataManager.export(outputStream, exportTimestamp).getOrThrow()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                ctx.toast(e.localizedMessage ?: e.stackTraceToString())
                            }
                        }
                    }
                }
            }
    }

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        screen.addPreference(R.string.export_user_data) {
            val ctx = requireContext()
            lifecycleScope.launch {
                lifecycleScope.withLoadingDialog(ctx) {
                    viewModel.fcitx.runOnReady {
                        save()
                    }
                }
                exportTimestamp = System.currentTimeMillis()
                exportLauncher.launch("fcitx5-android_${iso8601UTCDateTime(exportTimestamp)}.zip")
            }
        }
        screen.addPreference(R.string.import_user_data) {
            importLauncher.launch("application/zip")
        }
    }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }
}
