/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import arrow.core.getOrElse
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.main.modified.MySwitchPreference
import org.fcitx.fcitx5.android.utils.LongClickPreference
import org.fcitx.fcitx5.android.utils.buildDocumentsProviderIntent
import org.fcitx.fcitx5.android.utils.buildPrimaryStorageIntent
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigBool
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigCustom
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigEnum
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigEnumList
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigExternal
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigInt
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigKey
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigList
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.ConfigString
import org.fcitx.fcitx5.android.utils.config.ConfigType
import org.fcitx.fcitx5.android.utils.navigateWithAnim
import org.fcitx.fcitx5.android.utils.parcelableArray
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber

object PreferenceScreenFactory {

    private val hideKeyConfig by AppPrefs.getInstance().advanced.hideKeyConfig

    fun create(
        preferenceManager: PreferenceManager,
        fragmentManager: FragmentManager,
        raw: RawConfig,
        save: () -> Unit
    ): PreferenceScreen {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        val cfg = raw["cfg"]
        val desc = raw["desc"]
        val store = FcitxRawConfigStore(cfg)
        // TODO: needs some error handling
        val topLevelDesc = ConfigDescriptor.parseTopLevel(desc).getOrElse { throw it }
        screen.title = topLevelDesc.name
        topLevelDesc.values.forEach {
            general(context, fragmentManager, cfg.findByName(it.name), screen, it, store, save)
        }
        return screen
    }

    private fun general(
        context: Context,
        fragmentManager: FragmentManager,
        cfg: RawConfig?,
        screen: PreferenceScreen,
        descriptor: ConfigDescriptor<*, *>,
        store: PreferenceDataStore,
        save: () -> Unit
    ) {

        // Hide key related configs
        if (hideKeyConfig && ConfigType.pretty(descriptor.ty).contains("Key")) {
            return
        }

        if (descriptor is ConfigCustom) {
            custom(context, fragmentManager, cfg, screen, descriptor, save)
            return
        }

        fun stubPreference() = Preference(context).apply {
            summary =
                "${context.getString(R.string.unimplemented_type)} '${ConfigType.pretty(descriptor.ty)}'"
        }

        fun <T : Any> navigate(route: T): Boolean {
            return try {
                fragmentManager.primaryNavigationFragment!!.navigateWithAnim(route)
                true
            } catch (e: Exception) {
                Timber.w("Unable to navigate(route=$route): $e")
                false
            }
        }

        fun pinyinDictionary() = Preference(context).apply {
            setOnPreferenceClickListener {
                navigate(SettingsRoute.PinyinDict(""))
            }
        }

        fun punctuationEditor(title: String, lang: String?) = Preference(context).apply {
            setOnPreferenceClickListener {
                navigate(SettingsRoute.Punctuation(title, lang))
            }
        }

        fun quickPhraseEditor() = Preference(context).apply {
            setOnPreferenceClickListener {
                navigate(SettingsRoute.QuickPhraseList)
            }
        }

        fun tableInputMethod() = Preference(context).apply {
            setOnPreferenceClickListener {
                navigate(SettingsRoute.TableInputMethods)
            }
        }

        fun pinyinCustomPhrase() = Preference(context).apply {
            setOnPreferenceClickListener {
                navigate(SettingsRoute.PinyinCustomPhrase)
            }
        }

        fun rimeUserDataDir(title: String): Preference = LongClickPreference(context).apply {
            setOnPreferenceClickListener {
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(R.string.open_rime_user_data_dir)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        try {
                            context.startActivity(buildDocumentsProviderIntent())
                        } catch (e: Exception) {
                            context.toast(e)
                        }
                    }
                    .show()
                true
            }

            // make it a hidden option, because of compatibility issues
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setOnPreferenceLongClickListener {
                    try {
                        context.startActivity(buildPrimaryStorageIntent("data/rime"))
                    } catch (e: Exception) {
                        context.toast(e)
                    }
                }
            }
        }

        fun listPreference(subtype: ConfigType<*>): Preference = object : Preference(context) {
            override fun onClick() {
                navigate(SettingsRoute.ListConfig(cfg ?: RawConfig(), descriptor))
                fragmentManager.setFragmentResultListener(
                    descriptor.name,
                    fragmentManager.primaryNavigationFragment!!
                ) { _, v ->
                    cfg?.subItems = v.parcelableArray(descriptor.name)
                    if (callChangeListener(null)) {
                        notifyChanged()
                    }
                }
            }
        }.apply {
            if (subtype == ConfigType.TyKey) {
                summaryProvider = SummaryProvider<Preference> {
                    val keys = cfg?.subItems?.joinToString("\n") {
                        Key.parse(it.value).localizedString
                    }
                    if (keys.isNullOrEmpty()) context.getString(R.string.none) else keys
                }
            }
        }

        fun addonConfigPreference(addon: String) = Preference(context).apply {
            setOnPreferenceClickListener {
                navigate(
                    SettingsRoute.AddonConfig(descriptor.description ?: descriptor.name, addon)
                )
            }
        }

        when (descriptor) {
            is ConfigBool -> MySwitchPreference(context).apply {
                summary = descriptor.tooltip
                setDefaultValue(descriptor.defaultValue)
            }
            is ConfigEnum -> ListPreference(context).apply {
                entries = (descriptor.entriesI18n ?: descriptor.entries).toTypedArray()
                entryValues = descriptor.entries.toTypedArray()
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
                ConfigExternal.ETy.TableGlobal -> addonConfigPreference("table")
                ConfigExternal.ETy.AndroidTable -> tableInputMethod()
                ConfigExternal.ETy.PinyinCustomPhrase -> pinyinCustomPhrase()
                ConfigExternal.ETy.RimeUserDataDir -> rimeUserDataDir(
                    descriptor.description ?: descriptor.name
                )
                else -> stubPreference()
            }
            is ConfigInt -> {
                val min = descriptor.intMin
                val max = descriptor.intMax
                if (min != null && max != null && max - min <= 100) {
                    DialogSeekBarPreference(context).apply {
                        summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
                        descriptor.defaultValue?.let { setDefaultValue(it) }
                        this.min = min
                        this.max = max
                    }
                } else {
                    EditTextIntPreference(context).apply {
                        summaryProvider = EditTextIntPreference.SimpleSummaryProvider
                        descriptor.defaultValue?.let { setDefaultValue(it) }
                        min?.let { this.min = it }
                        max?.let { this.max = it }
                    }
                }
            }
            is ConfigKey -> FcitxKeyPreference(context).apply {
                summaryProvider = FcitxKeyPreference.SimpleSummaryProvider
                descriptor.defaultValue?.let { setDefaultValue(it) }
            }
            is ConfigList -> if (descriptor.ty.subtype in ListFragment.supportedSubtypes)
                listPreference(descriptor.ty.subtype)
            else
                stubPreference()
            is ConfigString -> EditTextPreference(context).apply {
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
            if (this is DialogPreference) {
                dialogTitle = title
                dialogMessage = descriptor.tooltip
            }
            setOnPreferenceChangeListener { _, _ ->
                // setOnPreferenceChangeListener runs before preferenceDataStore was updated,
                // post to save() to make sure store has been updated (hopefully)
                ContextCompat.getMainExecutor(context).execute {
                    save()
                }
                true
            }
            screen.addPreference(this)
        }
    }

    private fun custom(
        context: Context,
        fragmentManager: FragmentManager,
        cfg: RawConfig?,
        screen: PreferenceScreen,
        descriptor: ConfigCustom,
        save: () -> Unit
    ) {
        val subStore = FcitxRawConfigStore(cfg ?: RawConfig())
        val subPref = PreferenceCategory(context).apply {
            key = descriptor.name
            title = descriptor.description ?: descriptor.name
            isSingleLineTitle = false
            isIconSpaceReserved = false
        }
        screen.addPreference(subPref)
        descriptor.customTypeDef?.values?.forEach {
            general(context, fragmentManager, cfg?.findByName(it.name), screen, it, subStore, save)
        }
    }

}