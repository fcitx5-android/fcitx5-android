package org.fcitx.fcitx5.android.data.prefs

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen

abstract class ManagedPreferenceCategory(
    @StringRes private val title: Int,
    protected val sharedPreferences: SharedPreferences
) : ManagedPreferenceProvider() {

    protected fun switch(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Boolean,
        @StringRes
        summary: Int? = null,
        enableUiOn: () -> Boolean = { true },
    ): ManagedPreference.PBool {
        val pref = ManagedPreference.PBool(sharedPreferences, key, defaultValue)
        val ui = ManagedPreferenceUi.Switch(title, key, defaultValue, summary, enableUiOn)
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun <T : Any> list(
        @StringRes
        title: Int,
        key: String,
        defaultValue: T,
        codec: ManagedPreference.StringLikeCodec<T>,
        entries: List<Pair<String, T>>,
        enableUiOn: () -> Boolean = { true },
    ): ManagedPreference.PStringLike<T> {
        val pref = ManagedPreference.PStringLike(sharedPreferences, key, defaultValue, codec)
        val ui =
            ManagedPreferenceUi.StringList(title, key, defaultValue, codec, entries, enableUiOn)
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun int(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Int,
        min: Int,
        max: Int,
        unit: String = "",
        enableUiOn: () -> Boolean = { true },
    ): ManagedPreference.PInt {
        val pref = ManagedPreference.PInt(sharedPreferences, key, defaultValue)
        val ui =
            ManagedPreferenceUi.SeekBarInt(title, key, defaultValue, min, max, unit, enableUiOn)
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun twinInt(
        @StringRes
        title: Int,
        @StringRes
        label: Int,
        key: String,
        defaultValue: Int,
        @StringRes
        secondaryLabel: Int,
        secondaryKey: String,
        secondaryDefaultValue: Int,
        min: Int,
        max: Int,
        unit: String = "",
        enableUiOn: () -> Boolean = { true },
    ): Pair<ManagedPreference.PInt, ManagedPreference.PInt> {
        val primary = ManagedPreference.PInt(
            sharedPreferences,
            key, defaultValue,
        )
        val secondary = ManagedPreference.PInt(
            sharedPreferences,
            secondaryKey, secondaryDefaultValue
        )
        val ui = ManagedPreferenceUi.TwinSeekBarInt(
            title,
            label, key, defaultValue,
            secondaryLabel, secondaryKey, secondaryDefaultValue,
            min, max, unit, enableUiOn
        )
        primary.register()
        secondary.register()
        ui.registerUi()
        return primary to secondary
    }

    override fun createUi(screen: PreferenceScreen) {
        val category = PreferenceCategory(screen.context)
        category.isIconSpaceReserved = false
        category.setTitle(title)
        screen.addPreference(category)
        managedPreferencesUi.forEach {
            category.addPreference(it.createUi(screen.context).apply {
                isEnabled = it.enableUiOn()
            })
        }
    }
}