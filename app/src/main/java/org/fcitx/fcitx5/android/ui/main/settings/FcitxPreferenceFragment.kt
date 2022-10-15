package org.fcitx.fcitx5.android.ui.main.settings

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.forEach
import androidx.preference.isEmpty
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

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: FcitxConnection
        get() = viewModel.fcitx

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    private fun save() {
        if (!configLoaded) return
        lifecycleScope.launchOnFcitxReady(fcitx) {
            saveConfig(it, raw["cfg"])
        }
    }

    private val onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        save()
        true
    }

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        lifecycleScope.withLoadingDialog(context) {
            raw = fcitx.runOnReady { obtainConfig(this) }
            if (raw.findByName("cfg") != null && raw.findByName("desc") != null) {
                configLoaded = true
            } else {
                preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
                    addPreference(Preference(context).apply {
                        setTitle(R.string.config_addon_not_loaded)
                        isIconSpaceReserved = false
                    })
                }
                return@withLoadingDialog
            }
            val screen =
                PreferenceScreenFactory.create(preferenceManager, parentFragmentManager, raw)
            screen.forEach {
                it.onPreferenceChangeListener = onPreferenceChangeListener
            }
            if (screen.isEmpty()) {
                screen.addPreference(Preference(context).apply {
                    setTitle(R.string.no_config_options)
                    isIconSpaceReserved = false
                })
            }
            preferenceScreen = screen
            viewModel.disableAboutButton()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getPageTitle())
    }

}