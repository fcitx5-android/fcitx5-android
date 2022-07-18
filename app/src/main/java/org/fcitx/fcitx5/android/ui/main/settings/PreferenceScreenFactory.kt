package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import arrow.core.redeem
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.common.DialogSeekBarPreference
import org.fcitx.fcitx5.android.ui.main.settings.addon.AddonConfigFragment
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodConfigFragment
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.config.ConfigType

object PreferenceScreenFactory {

    private val hideKeyConfig by AppPrefs.getInstance().advanced.hideKeyConfig

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
            .redeem({ throw it }) {
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
        if (hideKeyConfig && ConfigType.pretty(descriptor.type).contains("Key"))
            return

        if (descriptor is ConfigDescriptor.ConfigCustom) {
            custom(context, fragmentManager, cfg, screen, descriptor)
            return
        }

        fun stubPreference() = Preference(context).apply {
            summary =
                "${context.getString(R.string.unimplemented_type)} '${ConfigType.pretty(descriptor.type)}'"
        }

        fun pinyinDictionary() = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_pinyinDictionaryFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_pinyinDictionaryFragment
                    else -> throw IllegalStateException("Can not navigate to pinyin dictionary from current fragment")
                }
                currentFragment.findNavController().navigate(action)
                true
            }
        }

        fun punctuationEditor(title: String) = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_punctuationEditorFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_punctuationEditorFragment
                    else -> throw IllegalStateException("Can not navigate to punctuation editor from current fragment")
                }
                currentFragment.findNavController().navigate(
                    action,
                    bundleOf(
                        PunctuationEditorFragment.TITLE to title
                    )
                )
                true
            }
        }

        fun quickPhraseEditor() = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_quickPhraseListFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_quickPhraseListFragment
                    else -> throw IllegalStateException("Can not navigate to quick phrase editor from current fragment")
                }
                currentFragment.findNavController().navigate(action)
                true
            }
        }

        fun listPreference() = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_listFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_listFragment
                    else -> throw IllegalStateException("Can not navigate to listFragment from current fragment")
                }
                currentFragment.findNavController().navigate(
                    action,
                    bundleOf(
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

        fun addonConfigPreference(addon: String) = Preference(context).apply {
            setOnPreferenceClickListener {
                val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_self
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_addonConfigFragment
                    else -> throw IllegalStateException("Can not navigate to addonConfigFragment from current fragment")
                }
                currentFragment.findNavController().navigate(
                    action,
                    bundleOf(
                        AddonConfigFragment.ARG_UNIQUE_NAME to addon,
                        AddonConfigFragment.ARG_NAME to (descriptor.description ?: descriptor.name)
                    )
                )
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
            is ConfigDescriptor.ConfigExternal -> when (descriptor.name) {
                "DictManager" -> pinyinDictionary()
                "Punctuation" -> punctuationEditor(descriptor.description ?: descriptor.name)
                "QuickPhrase", "Editor" -> quickPhraseEditor()
                "Chttrans" -> addonConfigPreference("chttrans")
                "TableGlobal" -> addonConfigPreference("table")
                else -> stubPreference()
            }
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