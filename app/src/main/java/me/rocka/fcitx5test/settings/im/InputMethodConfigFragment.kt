package me.rocka.fcitx5test.settings.im

import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.FcitxPreferenceFragment

class InputMethodConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String = requireStringArg(ARG_NAME)

    override fun obtainConfig(fcitx: Fcitx): RawConfig =
        fcitx.imConfig[requireStringArg(ARG_UNIQUE_NAME)]

    override fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.imConfig[requireStringArg(ARG_UNIQUE_NAME)] = newConfig
    }

    companion object {
        const val ARG_UNIQUE_NAME = "im"
        const val ARG_NAME = "im_"
    }
}