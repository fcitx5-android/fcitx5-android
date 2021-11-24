package me.rocka.fcitx5test.settings.global

import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.FcitxPreferenceFragment

class GlobalConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String =
        requireContext().resources.getString(R.string.global_options)

    override fun obtainConfig(fcitx: Fcitx): RawConfig = fcitx.globalConfig

    override fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.globalConfig = newConfig
    }

}