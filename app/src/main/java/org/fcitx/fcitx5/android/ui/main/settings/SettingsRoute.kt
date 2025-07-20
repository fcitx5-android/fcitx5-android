/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.ui.main.settings

import android.net.Uri
import android.os.Parcelable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.createGraph
import androidx.navigation.fragment.fragment
import androidx.savedstate.SavedState
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhrase
import org.fcitx.fcitx5.android.ui.main.AboutFragment
import org.fcitx.fcitx5.android.ui.main.DeveloperFragment
import org.fcitx.fcitx5.android.ui.main.LicensesFragment
import org.fcitx.fcitx5.android.ui.main.MainFragment
import org.fcitx.fcitx5.android.ui.main.PluginFragment
import org.fcitx.fcitx5.android.ui.main.settings.addon.AddonConfigFragment
import org.fcitx.fcitx5.android.ui.main.settings.addon.AddonListFragment
import org.fcitx.fcitx5.android.ui.main.settings.behavior.AdvancedSettingsFragment
import org.fcitx.fcitx5.android.ui.main.settings.behavior.CandidatesSettingsFragment
import org.fcitx.fcitx5.android.ui.main.settings.behavior.ClipboardSettingsFragment
import org.fcitx.fcitx5.android.ui.main.settings.behavior.KeyboardSettingsFragment
import org.fcitx.fcitx5.android.ui.main.settings.behavior.SymbolSettingsFragment
import org.fcitx.fcitx5.android.ui.main.settings.global.GlobalConfigFragment
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodConfigFragment
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodListFragment
import org.fcitx.fcitx5.android.ui.main.settings.theme.ThemeFragment
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.parcelable
import kotlin.reflect.typeOf

@Parcelize
sealed class SettingsRoute : Parcelable {

    /* ========== Index ========== */

    @Serializable
    data object Index : SettingsRoute()

    /* ========== Fcitx ========== */

    @Serializable
    data object GlobalConfig : SettingsRoute()

    @Serializable
    data object InputMethodList : SettingsRoute()

    @Serializable
    data class InputMethodConfig(val name: String, val uniqueName: String) : SettingsRoute()

    @Serializable
    data object AddonList : SettingsRoute()

    @Serializable
    data class AddonConfig(val name: String, val uniqueName: String) : SettingsRoute()

    /* ========== Android ========== */

    @Serializable
    data object Theme : SettingsRoute()

    @Serializable
    data object VirtualKeyboard : SettingsRoute()

    @Serializable
    data object CandidatesWindow : SettingsRoute()

    @Serializable
    data object Clipboard : SettingsRoute()

    @Serializable
    data object Symbol : SettingsRoute()

    @Serializable
    data object Plugin : SettingsRoute()

    @Serializable
    data object Advanced : SettingsRoute()

    @Serializable
    data object Developer : SettingsRoute()

    @Serializable
    data object License : SettingsRoute()

    @Serializable
    data object About : SettingsRoute()

    /* ========== External ========== */

    @Serializable
    data class ListConfig(val params: Params) : SettingsRoute() {
        @Parcelize
        @Serializable
        data class Params(val cfg: RawConfig, val desc: ConfigDescriptor<*, *>) : Parcelable {
            companion object {
                // https://developer.android.com/guide/navigation/design/kotlin-dsl#custom-types
                val NavType = object : NavType<Params>(isNullableAllowed = false) {
                    override fun put(bundle: SavedState, key: String, value: Params) {
                        bundle.putParcelable(key, value)
                    }

                    override fun get(bundle: SavedState, key: String): Params? {
                        return bundle.parcelable<Params>(key)
                    }

                    override fun serializeAsValue(value: Params): String {
                        // Serialized values must always be Uri encoded
                        return Uri.encode(Json.encodeToString(value))
                    }

                    override fun parseValue(value: String): Params {
                        // Navigation decodes the string before passing it to parseValue()
                        return Json.decodeFromString(value)
                    }
                }
            }
        }

        constructor(cfg: RawConfig, desc: ConfigDescriptor<*, *>) : this(Params(cfg, desc))

        val desc: ConfigDescriptor<*, *>
            get() = params.desc
        val cfg: RawConfig
            get() = params.cfg
    }

    @Serializable
    data class PinyinDict(val uri: String? = null) : SettingsRoute() {
        constructor(uri: Uri) : this(uri.toString())
    }

    @Serializable
    data class Punctuation(val title: String, val lang: String? = null) : SettingsRoute()

    @Serializable
    data object QuickPhraseList : SettingsRoute()

    @Serializable
    data class QuickPhraseEdit(val param: Param) : SettingsRoute() {
        constructor(quickPhrase: QuickPhrase) : this(Param(quickPhrase))

        @Serializable
        @Parcelize
        data class Param(
            val quickPhrase: QuickPhrase
        ) : Parcelable {
            companion object {
                val NavType = object : NavType<Param>(isNullableAllowed = false) {
                    override fun put(bundle: SavedState, key: String, value: Param) {
                        bundle.putParcelable(key, value)
                    }

                    override fun get(bundle: SavedState, key: String): Param? {
                        return bundle.parcelable<Param>(key)
                    }

                    override fun serializeAsValue(value: Param): String {
                        return Uri.encode(Json.encodeToString(value))
                    }

                    override fun parseValue(value: String): Param {
                        return Json.decodeFromString(value)
                    }
                }
            }
        }
    }

    @Serializable
    data object TableInputMethods : SettingsRoute()

    @Serializable
    data object PinyinCustomPhrase : SettingsRoute()

    companion object {
        fun createGraph(controller: NavController) = controller.createGraph(Index) {
            val ctx = controller.context

            /* ========== Index ========== */

            fragment<MainFragment, Index> {
                label = ctx.getString(R.string.app_name)
            }

            /* ========== Fcitx ========== */

            fragment<GlobalConfigFragment, GlobalConfig>()
            fragment<InputMethodListFragment, InputMethodList> {
                label = ctx.getString(R.string.input_methods)
            }
            fragment<InputMethodConfigFragment, InputMethodConfig>()
            fragment<AddonListFragment, AddonList> {
                label = ctx.getString(R.string.addons)
            }
            fragment<AddonConfigFragment, AddonConfig>()

            /* ========== Android ========== */

            fragment<ThemeFragment, Theme> {
                label = ctx.getString(R.string.theme)
            }
            fragment<KeyboardSettingsFragment, VirtualKeyboard> {
                label = ctx.getString(R.string.virtual_keyboard)
            }
            fragment<CandidatesSettingsFragment, CandidatesWindow> {
                label = ctx.getString(R.string.candidates_window)
            }
            fragment<ClipboardSettingsFragment, Clipboard> {
                label = ctx.getString(R.string.clipboard)
            }
            fragment<SymbolSettingsFragment, Symbol> {
                label = ctx.getString(R.string.emoji_and_symbols)
            }
            fragment<PluginFragment, Plugin> {
                label = ctx.getString(R.string.plugins)
            }
            fragment<AdvancedSettingsFragment, Advanced> {
                label = ctx.getString(R.string.advanced)
            }
            fragment<DeveloperFragment, Developer> {
                label = ctx.getString(R.string.developer)
            }
            fragment<LicensesFragment, License> {
                label = ctx.getString(R.string.license)
            }
            fragment<AboutFragment, About> {
                label = ctx.getString(R.string.about)
            }

            /* ========== External ========== */

            fragment<ListFragment, ListConfig>(
                typeMap = mapOf(typeOf<ListConfig.Params>() to ListConfig.Params.NavType)
            )
            fragment<PinyinDictionaryFragment, PinyinDict> {
                label = ctx.getString(R.string.pinyin_dict)
            }
            fragment<PunctuationEditorFragment, Punctuation>()
            fragment<QuickPhraseListFragment, QuickPhraseList> {
                label = ctx.getString(R.string.quickphrase_editor)
            }
            fragment<QuickPhraseEditFragment, QuickPhraseEdit>(
                typeMap = mapOf(typeOf<QuickPhraseEdit.Param>() to QuickPhraseEdit.Param.NavType)
            )
            fragment<TableInputMethodFragment, TableInputMethods> {
                label = ctx.getString(R.string.table_im)
            }
            fragment<PinyinCustomPhraseFragment, PinyinCustomPhrase>()
        }
    }
}
