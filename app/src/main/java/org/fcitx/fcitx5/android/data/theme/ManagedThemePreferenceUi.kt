package org.fcitx.fcitx5.android.data.theme

import android.content.Context
import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceUi
import org.fcitx.fcitx5.android.ui.common.ThemeSelectPreference

class ManagedThemePreferenceUi(
    @StringRes
    val title: Int,
    key: String,
    val defaultValue: Theme,
    @StringRes
    val summary: Int? = null,
    enableUiOn: () -> Boolean = { true },
) : ManagedPreferenceUi<ThemeSelectPreference>(key, enableUiOn) {
    override fun createUi(context: Context): ThemeSelectPreference =
        ThemeSelectPreference(context, defaultValue).apply {
            key = this@ManagedThemePreferenceUi.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            if (this@ManagedThemePreferenceUi.summary != null)
                setSummary(this@ManagedThemePreferenceUi.summary)
            setTitle(this@ManagedThemePreferenceUi.title)
        }
}