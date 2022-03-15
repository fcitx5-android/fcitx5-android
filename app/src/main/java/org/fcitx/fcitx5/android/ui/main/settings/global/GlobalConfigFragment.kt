package org.fcitx.fcitx5.android.ui.main.settings.global

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.main.settings.FcitxPreferenceFragment

class GlobalConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String =
        requireContext().resources.getString(R.string.global_options)

    override suspend fun obtainConfig(fcitx: Fcitx): RawConfig = fcitx.getGlobalConfig()

    override suspend fun saveConfig(fcitx: Fcitx, newConfig: RawConfig) =
        fcitx.setGlobalConfig(newConfig)

}