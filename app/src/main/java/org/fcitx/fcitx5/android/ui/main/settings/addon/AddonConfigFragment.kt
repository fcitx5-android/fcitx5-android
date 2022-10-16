package org.fcitx.fcitx5.android.ui.main.settings.addon

import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.main.settings.FcitxPreferenceFragment

class AddonConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String = requireStringArg(ARG_NAME)

    override suspend fun obtainConfig(fcitx: FcitxAPI): RawConfig =
        fcitx.getAddonConfig(requireStringArg(ARG_UNIQUE_NAME))

    override suspend fun saveConfig(fcitx: FcitxAPI, newConfig: RawConfig) {
        fcitx.setAddonConfig(requireStringArg(ARG_UNIQUE_NAME), newConfig)
    }

    companion object {
        const val ARG_UNIQUE_NAME = "addon"
        const val ARG_NAME = "addon_"
    }

}