package me.rocka.fcitx5test.settings

import android.content.ServiceConnection
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import me.rocka.fcitx5test.MainViewModel
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig

abstract class FcitxPreferenceFragment : PreferenceFragmentCompat() {
    abstract fun getPageTitle(): String
    abstract fun obtainConfig(fcitx: Fcitx): RawConfig
    abstract fun saveConfig(fcitx: Fcitx, newConfig: RawConfig)

    private lateinit var fcitx: Fcitx
    private lateinit var raw: RawConfig
    private var connection: ServiceConnection? = null
    private val viewModel: MainViewModel by activityViewModels()

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        viewModel.setToolbarTitle(getPageTitle())
        viewModel.enableToolbarSaveButton {
            saveConfig(fcitx, raw["cfg"])
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }
        connection = requireActivity().bindFcitxDaemon {
            fcitx = getFcitxInstance()
            // block UI until fcitx is ready
            onReady {
                raw = obtainConfig(fcitx)
                preferenceScreen = PreferenceScreenFactory.create(preferenceManager, raw)
            }
        }
    }

    override fun onPause() {
        saveConfig(fcitx, raw["cfg"])
        super.onPause()
    }

    override fun onDestroy() {
        connection?.let { requireActivity().unbindService(it) }
        super.onDestroy()
    }
}