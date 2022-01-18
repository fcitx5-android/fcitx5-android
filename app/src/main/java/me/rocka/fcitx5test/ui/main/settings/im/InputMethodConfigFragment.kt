package me.rocka.fcitx5test.ui.main.settings.im

import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.ui.main.settings.FcitxPreferenceFragment

class InputMethodConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String = requireStringArg(ARG_NAME)

    override suspend fun obtainConfig(fcitx: Fcitx): RawConfig =
        fcitx.getImConfig(requireStringArg(ARG_UNIQUE_NAME))

    override suspend fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.setImConfig(requireStringArg(ARG_UNIQUE_NAME), newConfig)
    }

    companion object {
        const val ARG_UNIQUE_NAME = "im"
        const val ARG_NAME = "im_"
    }
}