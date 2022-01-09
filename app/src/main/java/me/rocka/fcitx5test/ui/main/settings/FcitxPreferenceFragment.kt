package me.rocka.fcitx5test.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.ui.main.MainViewModel

abstract class FcitxPreferenceFragment : PreferenceFragmentCompat() {
    abstract fun getPageTitle(): String
    abstract fun obtainConfig(fcitx: Fcitx): RawConfig
    abstract fun saveConfig(fcitx: Fcitx, newConfig: RawConfig)

    private lateinit var raw: RawConfig

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.enableToolbarSaveButton {
            saveConfig(fcitx, raw["cfg"])
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        raw = obtainConfig(fcitx)
        preferenceScreen =
            PreferenceScreenFactory.create(preferenceManager, parentFragmentManager, raw)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getPageTitle())
    }

    override fun onPause() {
        saveConfig(fcitx, raw["cfg"])
        super.onPause()
    }

}