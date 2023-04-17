package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.core.data.FileSource
import org.fcitx.fcitx5.android.core.data.PluginLoadFailed
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.utils.addCategory
import org.fcitx.fcitx5.android.utils.addPreference

class PluginFragment : PaddingPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.launch {
            DataManager.waitSynced()
            preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
                addCategory(R.string.loaded) {
                    isIconSpaceReserved = false
                    DataManager.getLoadedPlugins().forEach {
                        addPreference(it.name, "${it.versionName}\n${it.description}") {
                            Toast.makeText(context, it.packageName, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                DataManager.getFailedPlugins().also { failedPlugins ->
                    if (failedPlugins.isEmpty()) return@also
                    addCategory(R.string.failed) {
                        isIconSpaceReserved = false
                        failedPlugins.forEach { (packageName, reason) ->
                            val summary = when (reason) {
                                is PluginLoadFailed.DataDescriptorParseError -> {
                                    getString(R.string.invalid_data_descriptor)
                                }
                                is PluginLoadFailed.MissingDataDescriptor -> {
                                    getString(R.string.missing_data_descriptor)
                                }
                                PluginLoadFailed.MissingPluginDescriptor -> {
                                    getString(R.string.missing_plugin_descriptor)
                                }
                                is PluginLoadFailed.PathConflict -> {
                                    getString(
                                        R.string.path_conflict,
                                        reason.path,
                                        when (val src = reason.existingSrc) {
                                            FileSource.Main -> R.string.main_program
                                            is FileSource.Plugin -> src.descriptor.name
                                        }
                                    )
                                }
                                is PluginLoadFailed.PluginAPIIncompatible -> {
                                    getString(R.string.incompatible_api, reason.api)
                                }
                                PluginLoadFailed.PluginDescriptorParseError -> {
                                    getString(R.string.invalid_plugin_descriptor)
                                }
                            }
                            addPreference(packageName, summary)
                        }
                    }
                }
            }
        }
    }

    private suspend fun DataManager.waitSynced() = suspendCancellableCoroutine {
        if (synced)
            it.resumeWith(Result.success(Unit))
        else
            addOnNextSyncedCallback {
                it.resumeWith(Result.success(Unit))
            }
    }
}