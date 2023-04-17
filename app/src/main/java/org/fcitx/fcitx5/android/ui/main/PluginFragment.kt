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

class PluginFragment : PaddingPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.launch {
            DataManager.waitSynced()
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)
            val loadedCategory = PreferenceCategory(context).apply {
                isIconSpaceReserved = false
                setTitle(R.string.loaded)
            }
            screen.addPreference(loadedCategory)
            DataManager.getLoadedPlugins().forEach {
                loadedCategory.addPreference(
                    Preference(context).apply {
                        isIconSpaceReserved = false
                        title = it.name
                        summary = "${it.versionName}\n${it.description}"
                        setOnPreferenceClickListener { _ ->
                            Toast.makeText(context, it.packageName, Toast.LENGTH_SHORT).show()
                            true
                        }
                    })
            }
            val failedCategory = PreferenceCategory(context).apply {
                isIconSpaceReserved = false
                setTitle(R.string.failed)
            }
            screen.addPreference(failedCategory)
            DataManager.getFailedPlugins().forEach { (packageName, reason) ->
                failedCategory.addPreference(
                    Preference(context).apply {
                        isIconSpaceReserved = false
                        isSingleLineTitle = false
                        title = packageName
                        setOnPreferenceClickListener { _ ->
                            Toast.makeText(context, packageName, Toast.LENGTH_SHORT).show()
                            true
                        }
                        when (reason) {
                            is PluginLoadFailed.DataDescriptorParseError -> {
                                setSummary(R.string.invalid_data_descriptor)
                            }
                            is PluginLoadFailed.MissingDataDescriptor -> {
                                setSummary(R.string.missing_data_descriptor)
                            }
                            PluginLoadFailed.MissingPluginDescriptor -> {
                                setSummary(R.string.missing_plugin_descriptor)
                            }
                            is PluginLoadFailed.PathConflict -> {
                                summary = getString(
                                    R.string.path_conflict,
                                    reason.path,
                                    reason.existingSrc.let { src ->
                                        when (src) {
                                            FileSource.Main -> R.string.main_program
                                            is FileSource.Plugin -> src.descriptor.name
                                        }
                                    })
                            }
                            is PluginLoadFailed.PluginAPIIncompatible -> {
                                summary = getString(R.string.incompatible_api, reason.api)
                            }
                            PluginLoadFailed.PluginDescriptorParseError -> {
                                setSummary(R.string.invalid_plugin_descriptor)
                            }
                        }
                    })
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