/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import androidx.preference.PreferenceDataStore
import org.fcitx.fcitx5.android.core.RawConfig

class FcitxRawConfigStore(private var cfg: RawConfig) : PreferenceDataStore() {
    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (key == null) return defValue
        return cfg[key].value == "True"
    }

    override fun putBoolean(key: String?, value: Boolean) {
        if (key == null) return
        cfg[key].value = if (value) "True" else "False"
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (key == null) return defValue
        return cfg[key].value.toInt()
    }

    override fun putInt(key: String?, value: Int) {
        if (key == null) return
        cfg[key].value = value.toString()
    }

    override fun getString(key: String?, defValue: String?): String? {
        if (key == null) return defValue
        return cfg[key].value
    }

    override fun putString(key: String?, value: String?) {
        if (key == null) return
        cfg[key].value = value ?: ""
    }

}
