package me.rocka.fcitx5test.settings

import androidx.preference.PreferenceDataStore
import me.rocka.fcitx5test.native.RawConfig

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
