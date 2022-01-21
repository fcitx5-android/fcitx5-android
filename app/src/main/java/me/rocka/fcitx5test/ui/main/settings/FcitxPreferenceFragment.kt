package me.rocka.fcitx5test.ui.main.settings

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.ui.common.withLoadingDialog
import me.rocka.fcitx5test.ui.main.MainViewModel

abstract class FcitxPreferenceFragment : PreferenceFragmentCompat() {
    abstract fun getPageTitle(): String
    abstract suspend fun obtainConfig(fcitx: Fcitx): RawConfig
    abstract suspend fun saveConfig(fcitx: Fcitx, newConfig: RawConfig)

    private lateinit var raw: RawConfig

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.withLoadingDialog(requireContext()) {
            raw = obtainConfig(fcitx)
            preferenceScreen =
                PreferenceScreenFactory.create(preferenceManager, parentFragmentManager, raw)
            viewModel.enableToolbarSaveButton {
                lifecycleScope.launch {
                    saveConfig(fcitx, raw["cfg"])
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getPageTitle())
    }

    override fun onPause() {
        if (this::raw.isInitialized)
            lifecycleScope.launch {
                saveConfig(fcitx, raw["cfg"])
            }
        super.onPause()
    }

}