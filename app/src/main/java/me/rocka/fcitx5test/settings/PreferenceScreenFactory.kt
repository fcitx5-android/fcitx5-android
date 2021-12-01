package me.rocka.fcitx5test.settings

import android.content.Context
import androidx.preference.*
import cn.berberman.girls.utils.either.otherwise
import cn.berberman.girls.utils.either.then
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.settings.parsed.ConfigDescriptor
import me.rocka.fcitx5test.settings.parsed.ConfigType

object PreferenceScreenFactory {

    fun create(preferenceManager: PreferenceManager, raw: RawConfig): PreferenceScreen {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        val cfg = raw["cfg"]
        val desc = raw["desc"]
        val store = FcitxRawConfigStore(cfg)

        ConfigDescriptor
            .parseTopLevel(desc)
            .otherwise { throw it }
            .then {
                screen.title = it.name
                it.values.forEach { d ->
                    general(context, cfg, screen, d, store)
                }
            }

        return screen
    }


    private fun general(
        context: Context,
        cfg: RawConfig,
        screen: PreferenceScreen,
        descriptor: ConfigDescriptor<*, *>,
        store: PreferenceDataStore
    ) {
        if (descriptor is ConfigDescriptor.ConfigCustom) {
            custom(context, cfg, screen, descriptor)
            return
        }

        fun stubPreference() = Preference(context).apply {
            summary = "⛔ Unimplemented type '${ConfigType.pretty(descriptor.type)}'"
        }

        when (descriptor) {
            is ConfigDescriptor.ConfigBool -> SwitchPreferenceCompat(context).apply {
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigDescriptor.ConfigEnum -> ListPreference(context).apply {
                entries = (descriptor.entriesI18n ?: descriptor.entries).toTypedArray()
                entryValues = descriptor.entries.toTypedArray()
                dialogTitle = descriptor.description ?: descriptor.name
                summaryProvider = Preference.SummaryProvider { pref: ListPreference ->
                    entries[entryValues.indexOf(pref.value)]
                }
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigDescriptor.ConfigEnumList -> stubPreference() // TODO
            is ConfigDescriptor.ConfigExternal -> stubPreference()
            is ConfigDescriptor.ConfigInt -> SeekBarPreference(context).apply {
                showSeekBarValue = true
                setDefaultValue(descriptor.defaultValue)
                descriptor.intMin?.let { min = it }
                descriptor.intMax?.let { max = it }
            }
            is ConfigDescriptor.ConfigKey -> stubPreference()
            is ConfigDescriptor.ConfigList -> stubPreference() // TODO
            is ConfigDescriptor.ConfigString -> EditTextPreference(context).apply {
                dialogTitle = descriptor.description ?: descriptor.name
                summaryProvider = Preference.SummaryProvider { pref: EditTextPreference ->
                    pref.text
                }
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigDescriptor.ConfigCustom -> throw IllegalAccessException("Impossible!")
        }.apply {
            key = descriptor.name
            title = descriptor.description ?: descriptor.name
            isSingleLineTitle = false
            isIconSpaceReserved = false
            preferenceDataStore = store
        }.let {
            screen.addPreference(it)
        }
    }

    private fun custom(
        context: Context,
        cfg: RawConfig,
        screen: PreferenceScreen,
        descriptor: ConfigDescriptor.ConfigCustom
    ) {
        val subStore = FcitxRawConfigStore(cfg[descriptor.name])
        val subPref = PreferenceCategory(context).apply {
            key = descriptor.name
            title = descriptor.description ?: descriptor.name
            isSingleLineTitle = false
            isIconSpaceReserved = false
        }
        screen.addPreference(subPref)
        descriptor.customTypeDef!!.values.forEach { general(context, cfg[descriptor.name], screen, it, subStore) }
    }


}