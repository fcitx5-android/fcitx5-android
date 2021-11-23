package me.rocka.fcitx5test.settings.im

import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.FcitxPreferenceFragment

class InputMethodConfigFragment : FcitxPreferenceFragment() {

    override fun obtainConfig(fcitx: Fcitx): RawConfig = fcitx.imConfig[requireStringArg(ARG_NAME)]

    override fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.imConfig[requireStringArg(ARG_NAME)] = newConfig
    }

    companion object {
        const val ARG_NAME = "im"
    }
}