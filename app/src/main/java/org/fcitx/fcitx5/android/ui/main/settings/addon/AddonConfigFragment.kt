/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.addon

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.main.settings.FcitxPreferenceFragment

class AddonConfigFragment : FcitxPreferenceFragment() {
    override fun getPageTitle(): String = requireStringArg(ARG_NAME)

    override suspend fun obtainConfig(fcitx: FcitxAPI): RawConfig {
        val addon = requireStringArg(ARG_UNIQUE_NAME)
        val raw = fcitx.getAddonConfig(addon)
        if (addon == "table") {
            // append android specific "Manage Table Input Methods" to config of table addon
            raw.findByName("desc")?.findByName("TableGlobalConfig")?.let {
                it.subItems = (it.subItems ?: emptyArray()) + RawConfig(
                    "AndroidTable", subItems = arrayOf(
                        RawConfig("Type", "External"),
                        RawConfig("Description", getString(R.string.manage_table_im))
                    )
                )
            }
        }
        return raw
    }

    override suspend fun saveConfig(fcitx: FcitxAPI, newConfig: RawConfig) {
        fcitx.setAddonConfig(requireStringArg(ARG_UNIQUE_NAME), newConfig)
    }

    companion object {
        const val ARG_UNIQUE_NAME = "addon"
        const val ARG_NAME = "addon_"
    }

}