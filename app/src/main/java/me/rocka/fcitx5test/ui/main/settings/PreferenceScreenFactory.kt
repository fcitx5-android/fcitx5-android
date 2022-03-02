package me.rocka.fcitx5test.ui.main.settings

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import cn.berberman.girls.utils.either.otherwise
import cn.berberman.girls.utils.either.then
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.RawConfig
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.ui.common.DialogSeekBarPreference
import me.rocka.fcitx5test.ui.main.settings.addon.AddonConfigFragment
import me.rocka.fcitx5test.ui.main.settings.im.InputMethodConfigFragment
import me.rocka.fcitx5test.utils.config.ConfigDescriptor
import me.rocka.fcitx5test.utils.config.ConfigType

object PreferenceScreenFactory {

    private val hideKeyConfig by Prefs.getInstance().hideKeyConfig

    fun create(
        preferenceManager: PreferenceManager,
        fragmentManager: FragmentManager,
        raw: RawConfig
    ): PreferenceScreen {
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
                    general(context, fragmentManager, cfg, screen, d, store)
                }
            }

        return screen
    }

    private fun general(
        context: Context,
        fragmentManager: FragmentManager,
        cfg: RawConfig,
        screen: PreferenceScreen,
        descriptor: ConfigDescriptor<*, *>,
        store: PreferenceDataStore
    ) {

        // Hide key related configs
        if (hideKeyConfig && ConfigType.pretty(descriptor.type)
                .contains("Key")
        )
            return

        if (descriptor is ConfigDescriptor.ConfigCustom) {
            custom(context, fragmentManager, cfg, screen, descriptor)
            return
        }

        fun stubPreference() = Preference(context).apply {
            summary = "⛔ Unimplemented type '${ConfigType.pretty(descriptor.type)}'"
        }

        fun pinyinDictionary() = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment =
                    fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_pinyinDictionaryFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_pinyinDictionaryFragment
                    else -> throw IllegalStateException("Can not navigate to pinyin dictionary from current fragment")
                }
                currentFragment.findNavController().navigate(action)
                true
            }
        }

        fun listPreference() = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment =
                    fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_listFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_listFragment
                    else -> throw IllegalStateException("Can not navigate to listFragment from current fragment")
                }
                currentFragment.findNavController().navigate(
                    action, bundleOf(
                        ListFragment.ARG_CFG to cfg[descriptor.name],
                        ListFragment.ARG_DESC to descriptor,
                    )
                )
                fragmentManager.setFragmentResultListener(
                    descriptor.name,
                    currentFragment
                ) { _, v ->
                    cfg[descriptor.name].subItems = (v[descriptor.name] as RawConfig).subItems
                }
                true
            }
        }

        when (descriptor) {
            is ConfigDescriptor.ConfigBool -> SwitchPreferenceCompat(context).apply {
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigDescriptor.ConfigEnum -> ListPreference(context).apply {
                entries = (descriptor.entriesI18n ?: descriptor.entries).toTypedArray()
                entryValues = descriptor.entries.toTypedArray()
                dialogTitle = descriptor.description ?: descriptor.name
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigDescriptor.ConfigEnumList -> listPreference()
            is ConfigDescriptor.ConfigExternal -> if (descriptor.name == "DictManager") pinyinDictionary() else stubPreference()
            is ConfigDescriptor.ConfigInt -> DialogSeekBarPreference(context).apply {
                summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
                descriptor.defaultValue?.let { defaultValue = it }
                descriptor.intMin?.let { min = it }
                descriptor.intMax?.let { max = it }
            }
            is ConfigDescriptor.ConfigKey -> stubPreference()
            is ConfigDescriptor.ConfigList -> if (descriptor.type.subtype in ListFragment.supportedSubtypes)
                listPreference()
            else
                stubPreference()
            is ConfigDescriptor.ConfigString -> EditTextPreference(context).apply {
                dialogTitle = descriptor.description ?: descriptor.name
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
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
        fragmentManager: FragmentManager,
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
        descriptor.customTypeDef!!.values.forEach {
            general(
                context,
                fragmentManager,
                cfg[descriptor.name],
                screen,
                it,
                subStore
            )
        }
    }

}