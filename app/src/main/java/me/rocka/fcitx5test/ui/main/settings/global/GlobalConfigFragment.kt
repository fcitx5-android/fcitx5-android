package me.rocka.fcitx5test.ui.main.settings.global

import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.ui.main.settings.FcitxPreferenceFragment

class GlobalConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String =
        requireContext().resources.getString(R.string.global_options)

    override suspend fun obtainConfig(fcitx: Fcitx): RawConfig = fcitx.getGlobalConfig()

    override suspend fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) =
        fcitx.setGlobalConfig(newConfig)

}