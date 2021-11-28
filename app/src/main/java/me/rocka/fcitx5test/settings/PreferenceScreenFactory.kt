package me.rocka.fcitx5test.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.*
import me.rocka.fcitx5test.native.RawConfig

object PreferenceScreenFactory {

    private var hideKeyConfig = true
    private var onPrefChange: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private fun setupOnPrefChangeListener(sp: SharedPreferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
            when (key) {
                PreferenceKeys.HideKeyConfig -> {
                    hideKeyConfig = pref.getBoolean(key, true)
                }
            }
        }
        onPrefChange = listener
        sp.run {
            listener.onSharedPreferenceChanged(this, PreferenceKeys.HideKeyConfig)
            registerOnSharedPreferenceChangeListener(listener)
        }
    }

    fun create(preferenceManager: PreferenceManager, raw: RawConfig): PreferenceScreen {
        if (onPrefChange == null) {
            setupOnPrefChangeListener(preferenceManager.sharedPreferences)
        }
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        val cfg = raw["cfg"]
        val desc = raw["desc"]
        val store = FcitxRawConfigStore(cfg)
        desc.subItems?.get(0)?.subItems?.forEach { item ->
            item["Type"].value.let { type ->
                when {
                    type.contains("$") -> cfg[item.name].let { subCategory ->
                        val subStore = FcitxRawConfigStore(subCategory)
                        val subPref = PreferenceCategory(context).apply {
                            key = item.name
                            title = item.findByName("Description")?.value ?: item.name
                            isSingleLineTitle = false
                            isIconSpaceReserved = false
                        }
                        screen.addPreference(subPref)
                        desc[type].subItems?.forEach { item ->
                            createSingle(context, item, subStore)?.let { subPref.addPreference(it) }
                        }
                    }
                    type.isNotEmpty() -> {
                        createSingle(context, item, store)?.let { screen.addPreference(it) }
                    }
                    else -> {
                        Log.i(javaClass.name, "unexpected subItem '$item'")
                    }
                }
            }
        }
        return screen
    }

    private fun createSingle(
        context: Context,
        cfg: RawConfig, store: PreferenceDataStore
    ): Preference? {
        val type = cfg["Type"].value
        if (hideKeyConfig and type.endsWith("Key")) return null
        val itemDesc = cfg.findByName("Description")?.value ?: cfg.name
        val defValue = cfg.findByName("DefaultValue")?.value ?: ""
        return when (type) {
            "Boolean" -> SwitchPreferenceCompat(context).apply {
                setDefaultValue(defValue == "True")
            }
            "Integer" -> SeekBarPreference(context).apply {
                showSeekBarValue = true
                setDefaultValue(defValue.toInt())
                cfg.findByName("IntMin")?.value?.toInt()?.let { min = it }
                cfg.findByName("IntMax")?.value?.toInt()?.let { max = it }
            }
            "Enum" -> ListPreference(context).apply {
                val enums = cfg["Enum"].subItems?.map { it.value }?.toTypedArray() ?: arrayOf()
                val names = cfg["EnumI18n"].subItems?.map { it.value }?.toTypedArray() ?: arrayOf()
                entries = names
                entryValues = enums
                dialogTitle = itemDesc
                summaryProvider = Preference.SummaryProvider { pref: ListPreference ->
                    names[enums.indexOf(pref.value)]
                }
                setDefaultValue(defValue)
            }
            "String" -> EditTextPreference(context).apply {
                dialogTitle = itemDesc
                summaryProvider = Preference.SummaryProvider { pref: EditTextPreference ->
                    pref.text
                }
                setDefaultValue(defValue)
            }
            else -> Preference(context).apply {
                summary = "⛔ Unimplemented type '$type'"
            }
        }.apply {
            key = cfg.name
            title = itemDesc
            isSingleLineTitle = false
            isIconSpaceReserved = false
            preferenceDataStore = store
        }
    }
}