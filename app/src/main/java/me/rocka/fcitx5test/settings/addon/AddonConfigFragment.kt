package me.rocka.fcitx5test.settings.addon

import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.FcitxPreferenceFragment

class AddonConfigFragment : FcitxPreferenceFragment() {

    override fun obtainConfig(fcitx: Fcitx): RawConfig =
        fcitx.addonConfig[requireStringArg(ARG_NAME)]

    override fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.addonConfig[requireStringArg(ARG_NAME)] = newConfig
    }

    companion object {
        const val ARG_NAME = "addon"
    }

}