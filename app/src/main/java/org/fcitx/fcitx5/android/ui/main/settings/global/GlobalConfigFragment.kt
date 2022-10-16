package org.fcitx.fcitx5.android.ui.main.settings.global

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.main.settings.FcitxPreferenceFragment

class GlobalConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String =
        requireContext().resources.getString(R.string.global_options)

    override suspend fun obtainConfig(fcitx: FcitxAPI): RawConfig = fcitx.getGlobalConfig()

    override suspend fun saveConfig(fcitx: FcitxAPI, newConfig: RawConfig) =
        fcitx.setGlobalConfig(newConfig)

}