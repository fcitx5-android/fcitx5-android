package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import androidx.preference.Preference.SummaryProvider
import arrow.core.redeem
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.main.settings.addon.AddonConfigFragment
import org.fcitx.fcitx5.android.ui.main.settings.global.GlobalConfigFragment
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodConfigFragment
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.*
import org.fcitx.fcitx5.android.utils.config.ConfigType
import org.fcitx.fcitx5.android.utils.parcelableArray

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

        if (descriptor is ConfigCustom) {
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

        fun punctuationEditor(title: String, lang: String?) = Preference(context).apply {
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
                        PunctuationEditorFragment.TITLE to title,
                        PunctuationEditorFragment.LANG to lang
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

        fun listPreference(subtype: ConfigType<*>): Preference = object : Preference(context) {
            override fun onClick() {
                val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)!!
                val action = when (currentFragment) {
                    is GlobalConfigFragment -> R.id.action_globalConfigFragment_to_listFragment
                    is InputMethodConfigFragment -> R.id.action_imConfigFragment_to_listFragment
                    is AddonConfigFragment -> R.id.action_addonConfigFragment_to_listFragment
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
                    cfg[descriptor.name].subItems = v.parcelableArray(descriptor.name)
                    if (callChangeListener(null)) {
                        notifyChanged()
                    }
                }
            }
        }.apply {
            if (subtype == ConfigType.TyKey) {
                summaryProvider = SummaryProvider<Preference> {
                    val str = cfg[descriptor.name].subItems?.joinToString("\n") {
                        Key.parse(it.value).localizedString
                    } ?: ""
                    str.ifEmpty { context.getString(R.string.none) }
                }
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
            is ConfigBool -> SwitchPreferenceCompat(context).apply {
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigEnum -> ListPreference(context).apply {
                entries = (descriptor.entriesI18n ?: descriptor.entries).toTypedArray()
                entryValues = descriptor.entries.toTypedArray()
                dialogTitle = descriptor.description ?: descriptor.name
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigEnumList -> listPreference(ConfigType.TyEnum)
            is ConfigExternal -> when (descriptor.knownType) {
                ConfigExternal.ETy.PinyinDict -> pinyinDictionary()
                ConfigExternal.ETy.Punctuation -> punctuationEditor(
                    descriptor.description ?: descriptor.name,
                    // fcitx://config/addon/punctuation/punctuationmap/zh_CN
                    descriptor.uri?.substringAfterLast('/')
                )
                ConfigExternal.ETy.QuickPhrase -> quickPhraseEditor()
                ConfigExternal.ETy.Chttrans -> addonConfigPreference("chttrans")
                ConfigExternal.ETy.Table -> addonConfigPreference("table")
                else -> stubPreference()
            }
            is ConfigInt -> DialogSeekBarPreference(context).apply {
                summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
                descriptor.defaultValue?.let { setDefaultValue(it) }
                descriptor.intMin?.let { min = it }
                descriptor.intMax?.let { max = it }
            }
            is ConfigKey -> FcitxKeyPreference(context).apply {
                summaryProvider = FcitxKeyPreference.SimpleSummaryProvider
                descriptor.defaultValue?.let { setDefaultValue(it) }
            }
            is ConfigList -> if (descriptor.type.subtype in ListFragment.supportedSubtypes)
                listPreference(descriptor.type.subtype)
            else
                stubPreference()
            is ConfigString -> EditTextPreference(context).apply {
                dialogTitle = descriptor.description ?: descriptor.name
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigCustom -> throw IllegalAccessException("Impossible!")
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
        descriptor: ConfigCustom
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