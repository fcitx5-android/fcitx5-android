package org.fcitx.fcitx5.android.ui.main.settings.im

import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.main.settings.FcitxPreferenceFragment

class InputMethodConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String = requireStringArg(ARG_NAME)

    override suspend fun obtainConfig(fcitx: FcitxAPI): RawConfig {
        val im = requireStringArg(ARG_UNIQUE_NAME)
        val raw = fcitx.getImConfig(im)
        if (im == "pinyin") {
            // hide Shuangpin related options in Pinyin config UI
            val desc = raw["desc"]
            desc.findByName("PinyinEngineConfig")?.apply {
                subItems = subItems?.filter { !it.name.contains("Shuangpin") }?.toTypedArray()
            }
            desc.findByName("Fuzzy\$FuzzyConfig")?.apply {
                subItems = subItems?.filter { it.name != "PartialSp" }?.toTypedArray()
            }
        }
        return raw
    }

    override suspend fun saveConfig(fcitx: FcitxAPI, newConfig: RawConfig) {
        fcitx.setImConfig(requireStringArg(ARG_UNIQUE_NAME), newConfig)
    }

    companion object {
        const val ARG_UNIQUE_NAME = "im"
        const val ARG_NAME = "im_"
    }
}