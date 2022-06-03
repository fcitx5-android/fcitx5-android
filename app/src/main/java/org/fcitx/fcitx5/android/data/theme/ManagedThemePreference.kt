package org.fcitx.fcitx5.android.data.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.ui.common.ThemeSelectPreference

class ManagedThemePreference(
    sharedPreferences: SharedPreferences,
    key: String,
    defaultValue: Theme,
    enableUiOn: () -> Boolean,
    uiConfig: ThemeSelectPreference.() -> Unit
) : ManagedPreference<Theme, ThemeSelectPreference>(
    sharedPreferences, key, defaultValue, enableUiOn, uiConfig
) {
    override fun createUiProtected(context: Context): ThemeSelectPreference =
        ThemeSelectPreference(context, defaultValue).apply {
            key = this@ManagedThemePreference.key
            isIconSpaceReserved = false
        }

    override fun setValue(value: Theme) {
        sharedPreferences.edit { putString(key, value.name) }
    }

    override fun getValue(): Theme =
        sharedPreferences.getString(key, null)?.let { name ->
            ThemeManager.getAllThemes().find { it.name == name }
        } ?: defaultValue
}