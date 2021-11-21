package me.rocka.fcitx5test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.RawConfig

class SettingsActivity : AppCompatActivity() {
    private lateinit var raw: RawConfig
    private lateinit var fcitx: Fcitx

    class MySettingsFragment(val raw: RawConfig) : PreferenceFragmentCompat() {
        private val cfg = raw["cfg"]!!
        private val desc = raw["desc"]!!

        private fun createSinglePreference(cfg: RawConfig, store: PreferenceDataStore): Preference {
            val type = cfg["Type"]?.value!!
            val itemDesc = cfg["Description"]?.value ?: cfg.name
            val defValue = cfg["DefaultValue"]?.value ?: ""
            return when (type) {
                "Boolean" -> SwitchPreferenceCompat(context)
                "Integer" -> SeekBarPreference(context).apply {
                    showSeekBarValue = true
                    min = cfg["IntMin"]!!.value.toInt()
                    max = cfg["IntMax"]!!.value.toInt()
                }
                "Enum" -> DropDownPreference(context).apply {
                    val enums = cfg["Enum"]!!.subItems!!.map { it.value }.toTypedArray()
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

            val topStore = FcitxRawConfigStore(cfg)
            desc.subItems!![0].subItems!!.forEach { category ->
                val type = category["Type"]?.value!!
                if (type.contains("$")) {
                    val store = FcitxRawConfigStore(cfg[category.name]!!)
                    val catPref = PreferenceCategory(context).apply {
                        key = category.name
                        title = category["Description"]?.value ?: category.name
                        isSingleLineTitle = false
                        isIconSpaceReserved = false
                    }
                    screen.addPreference(catPref)
                    desc[type]!!.subItems!!.forEach { item ->
                        catPref.addPreference(createSinglePreference(item, store))
                    }
                } else {
                    screen.addPreference(createSinglePreference(category, topStore))
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
                "addon" -> fcitx.addonConfig[intent.getStringExtra("addon")!!]!!
                "im" -> fcitx.imConfig[intent.getStringExtra("im")!!]!!
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
        val newValue = raw["cfg"]!!
        when (intent.getStringExtra("type")) {
            "global" -> fcitx.globalConfig = newValue
            "addon" -> fcitx.addonConfig[intent.getStringExtra("addon")!!] = newValue
            "im" -> fcitx.imConfig[intent.getStringExtra("im")!!] = newValue
            else -> {}
        }
    }
}
