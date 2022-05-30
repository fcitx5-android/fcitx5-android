package org.fcitx.fcitx5.android.data.prefs

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen

abstract class ManagedPreferenceCategory(
    @StringRes private val title: Int,
    private val sharedPreferences: SharedPreferences
) : ManagedPreferenceProvider {
    override val managedPreferences =
        mutableMapOf<String, ManagedPreference<*, *>>()

    protected fun switch(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Boolean,
        enableUiOn: () -> Boolean = { true },
    ) = ManagedPreference.Switch(sharedPreferences, key, defaultValue, enableUiOn) {
        this.setTitle(title)
    }.also {
        managedPreferences[key] = it
    }

    protected fun <T : Any> list(
        @StringRes
        title: Int,
        key: String,
        defaultValue: T,
        codec: ManagedPreference.StringLikeCodec<T>,
        entries: List<Pair<String, T>>,
        enableUiOn: () -> Boolean = { true },
    ) = ManagedPreference.StringLikeList(
        sharedPreferences,
        key,
        defaultValue,
        codec,
        entries.map { it.second },
        enableUiOn
    ) {
        this.setTitle(title)
        this.entries = entries.map { it.first }.toTypedArray()
        this.setDialogTitle(title)
    }.also {
        managedPreferences[key] = it
    }

    protected fun list(
        @StringRes
        title: Int,
        key: String,
        defaultValue: String,
        entries: Array<String>,
        enableUiOn: () -> Boolean = { true },
    ) = ManagedPreference.StringList(
        sharedPreferences,
        key,
        defaultValue,
        entries,
        enableUiOn
    ) {
        this.setTitle(title)
        this.entries = entries
        this.setDialogTitle(title)
    }.also {
        managedPreferences[key] = it
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
    ) = ManagedPreference.SeekBarInt(
        sharedPreferences,
        key,
        defaultValue,
        enableUiOn,
    ) {
        this.setTitle(title)
        this.min = min
        this.max = max
        this.unit = unit
    }.also {
        managedPreferences[key] = it
    }

    override fun createUi(screen: PreferenceScreen) {
        val category = PreferenceCategory(screen.context)
        category.isIconSpaceReserved = false
        category.setTitle(title)
        screen.addPreference(category)
        managedPreferences.forEach {
            category.addPreference(it.value.createUi(screen.context).apply {
                isEnabled = it.value.enableUiOn()
            })
        }
    }
}