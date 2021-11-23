package me.rocka.fcitx5test.settings

import android.content.ServiceConnection
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig

abstract class FcitxPreferenceFragment : PreferenceFragmentCompat() {
    abstract fun obtainConfig(fcitx: Fcitx): RawConfig
    abstract fun saveConfig(fcitx: Fcitx, newConfig: RawConfig)

    private lateinit var fcitx: Fcitx
    private lateinit var raw: RawConfig
    private var connection: ServiceConnection? = null

    fun requireStringArg(key: String) =
        requireArguments().getString(key)
            ?: throw IllegalStateException("No $key found in bundle")

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        connection = requireActivity().bindFcitxDaemon {
            fcitx = it.getFcitxInstance()
            preferenceScreen = PreferenceScreenFactory.create(
                preferenceManager,
                raw
            )
        }

    }

    override fun onPause() {
        saveConfig(fcitx, raw)
        super.onPause()
    }

    override fun onDestroy() {
        connection?.let { requireActivity().unbindService(it) }
        super.onDestroy()
    }
}