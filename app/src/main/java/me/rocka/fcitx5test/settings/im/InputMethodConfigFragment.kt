package me.rocka.fcitx5test.settings.im

import android.util.Log
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.FcitxPreferenceFragment

class InputMethodConfigFragment : FcitxPreferenceFragment() {

    override fun obtainConfig(fcitx: Fcitx): RawConfig {
        Log.i(javaClass.name, "obtainConfig: ${requireStringArg(ARG_NAME)}")
        val raw = fcitx.imConfig[requireStringArg(ARG_NAME)]
        Log.i(javaClass.name, "obtainConfig: $raw")
        return raw
    }

    override fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.imConfig[requireStringArg(ARG_NAME)] = newConfig
    }

    companion object {
        const val ARG_NAME = "im"
    }
}