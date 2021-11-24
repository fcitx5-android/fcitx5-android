package me.rocka.fcitx5test.settings.addon

import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.FcitxPreferenceFragment

class AddonConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String =
        "${requireContext().resources.getString(R.string.addons_conf)}: ${requireStringArg(ARG_NAME)}"

    override fun obtainConfig(fcitx: Fcitx): RawConfig =
        fcitx.addonConfig[requireStringArg(ARG_UNIQUE_NAME)]

    override fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) {
        fcitx.addonConfig[requireStringArg(ARG_UNIQUE_NAME)] = newConfig
    }

    companion object {
        const val ARG_UNIQUE_NAME = "addon"
        const val ARG_NAME = "addon_"
    }

}