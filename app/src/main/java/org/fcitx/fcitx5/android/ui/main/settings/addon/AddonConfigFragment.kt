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
            val desc = raw["desc"]["TableGlobalConfig"]
            val androidTable = RawConfig(
                "AndroidTable", subItems = arrayOf(
                    RawConfig("Type", "External"),
                    RawConfig("Description", getString(R.string.manage_table_im))
                )
            )
            desc.subItems = (desc.subItems ?: arrayOf()) + androidTable
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