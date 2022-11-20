package org.fcitx.fcitx5.android.ui.main.settings

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.isEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.launchOnFcitxReady
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.ui.main.MainViewModel

abstract class FcitxPreferenceFragment : PaddingPreferenceFragment() {
    abstract fun getPageTitle(): String
    abstract suspend fun obtainConfig(fcitx: FcitxAPI): RawConfig
    abstract suspend fun saveConfig(fcitx: FcitxAPI, newConfig: RawConfig)

    private lateinit var raw: RawConfig
    private var configLoaded = false

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob)

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: FcitxConnection
        get() = viewModel.fcitx

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    private fun save() {
        if (!configLoaded) return
        scope.launchOnFcitxReady(fcitx) {
            saveConfig(it, raw["cfg"])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher
            .addCallback(this, object : OnBackPressedCallback(true) {
                // prevent "back" from navigating away from this Fragment when it's still saving
                override fun handleOnBackPressed() {
                    lifecycleScope.withLoadingDialog(requireContext(), R.string.saving) {
                        // complete the parent job and wait for all children
                        supervisorJob.complete()
                        supervisorJob.join()
                        scope.cancel()
                        findNavController().popBackStack()
                    }
                }
            })
    }

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        lifecycleScope.withLoadingDialog(context) {
            raw = fcitx.runOnReady { obtainConfig(this) }
            configLoaded = raw.findByName("cfg") != null && raw.findByName("desc") != null
            preferenceScreen = if (configLoaded) {
                PreferenceScreenFactory.create(
                    preferenceManager, parentFragmentManager, raw, ::save
                ).apply {
                    if (isEmpty()) {
                        addPreference(Preference(context).apply {
                            setTitle(R.string.no_config_options)
                            isIconSpaceReserved = false
                        })
                    }
                }
            } else {
                preferenceManager.createPreferenceScreen(context).apply {
                    addPreference(Preference(context).apply {
                        setTitle(R.string.config_addon_not_loaded)
                        isIconSpaceReserved = false
                    })
                }
            }
            viewModel.disableAboutButton()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getPageTitle())
    }

}