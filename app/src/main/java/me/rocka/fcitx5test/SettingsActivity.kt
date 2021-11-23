package me.rocka.fcitx5test

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig

class SettingsActivity : AppCompatActivity() {
    private lateinit var raw: RawConfig
    private lateinit var fcitx: Fcitx

    class MySettingsFragment(raw: RawConfig) : PreferenceFragmentCompat() {
        private val cfg = raw["cfg"]
        private val desc = raw["desc"]

        private fun createSinglePreference(cfg: RawConfig, store: PreferenceDataStore): Preference {
            val type = cfg["Type"].value
            val itemDesc = cfg.findByName("Description")?.value ?: cfg.name
            val defValue = cfg.findByName("DefaultValue")?.value ?: ""
            return when (type) {
                "Boolean" -> SwitchPreferenceCompat(context)
                "Integer" -> SeekBarPreference(context).apply {
                    showSeekBarValue = true
                    cfg.findByName("IntMin")?.value?.toInt()?.let { min = it }
                    cfg.findByName("IntMax")?.value?.toInt()?.let { max = it }
                }
                "Enum" -> DropDownPreference(context).apply {
                    val enums = cfg["Enum"].subItems?.map { it.value }?.toTypedArray() ?: arrayOf()
                    entries = enums
                    entryValues = enums
                    summary = store.getString(cfg.name, defValue)
                    setOnPreferenceChangeListener { pref, v ->
                        pref.summary = (v as String)
                        true
                    }
                }
                "String" -> EditTextPreference(context).apply {
                    summary = store.getString(cfg.name, defValue)
                    dialogTitle = itemDesc
                    setOnPreferenceChangeListener { pref, v ->
                        pref.summary = (v as Int).toString()
                        true
                    }
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

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

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
                                subPref.addPreference(createSinglePreference(item, subStore))
                            }
                        }
                        type.isNotEmpty() -> {
                            screen.addPreference(createSinglePreference(item, store))
                        }
                        else -> {
                            Log.i(javaClass.name, "unexpected subItem '$item'")
                        }
                    }
                }
            }

            preferenceScreen = screen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindFcitxDaemon {
            fcitx = it.getFcitxInstance()
            raw = when (intent.getStringExtra("type")) {
                "global" -> fcitx.globalConfig
                "addon" -> fcitx.addonConfig[intent.getStringExtra("addon") ?: ""]
                "im" -> fcitx.imConfig[intent.getStringExtra("im") ?: ""]
                else -> RawConfig(arrayOf())
            }
            setContentView(R.layout.activity_settings)
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, MySettingsFragment(raw))
                .commit()
        }

    }

    override fun onPause() {
        super.onPause()
        val newValue = raw["cfg"]
        when (intent.getStringExtra("type")) {
            "global" -> fcitx.globalConfig = newValue
            "addon" -> fcitx.addonConfig[intent.getStringExtra("addon") ?: ""] = newValue
            "im" -> fcitx.imConfig[intent.getStringExtra("im") ?: ""] = newValue
            else -> {}
        }
    }
}
