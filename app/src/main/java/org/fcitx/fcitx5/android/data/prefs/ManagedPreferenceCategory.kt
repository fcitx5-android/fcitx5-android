/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceScreen

abstract class ManagedPreferenceCategory(
    @StringRes val title: Int,
    protected val sharedPreferences: SharedPreferences
) : ManagedPreferenceProvider() {

    protected fun switch(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Boolean,
        @StringRes
        summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
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
        entryValues: List<T>,
        @StringRes
        entryLabels: List<Int>,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PStringLike<T> {
        val pref = ManagedPreference.PStringLike(sharedPreferences, key, defaultValue, codec)
        val ui = ManagedPreferenceUi.StringList(
            title, key, defaultValue, codec, entryValues, entryLabels, enableUiOn
        )
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun int(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Int,
        min: Int = 0,
        max: Int = Int.MAX_VALUE,
        unit: String = "",
        step: Int = 1,
        @StringRes
        defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PInt {
        val pref = ManagedPreference.PInt(sharedPreferences, key, defaultValue)
        // Int can overflow when min < 0 && max == Int.MAX_VALUE
        val ui = if ((max.toLong() - min.toLong()) / step.toLong() >= 240L)
            ManagedPreferenceUi.EditTextInt(
                title, key, defaultValue, min, max, unit, enableUiOn
            )
        else
            ManagedPreferenceUi.SeekBarInt(
                title, key, defaultValue, min, max, unit, step, defaultLabel, enableUiOn
            )
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
        step: Int = 1,
        @StringRes
        defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
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
            min, max, unit, step, defaultLabel, enableUiOn
        )
        primary.register()
        secondary.register()
        ui.registerUi()
        return primary to secondary
    }

    override fun createUi(screen: PreferenceScreen) {
        val ctx = screen.context
        managedPreferencesUi.forEach {
            screen.addPreference(it.createUi(ctx).apply {
                isEnabled = it.isEnabled()
            })
        }
    }
}