package org.fcitx.fcitx5.android.data.prefs

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import org.fcitx.fcitx5.android.ui.main.settings.DialogSeekBarPreference

abstract class ManagedPreferenceUi<T : Preference>(
    // not related to enableUiOn, unused for now
    dependingKeys: Set<String>,
    val enableUiOn: () -> Boolean
) {

    val key = dependingKeys.joinToString("+")

    abstract fun createUi(context: Context): T

    class Switch(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Boolean,
        @StringRes
        val summary: Int? = null,
        enableUiOn: () -> Boolean = { true }
    ) : ManagedPreferenceUi<SwitchPreference>(
        setOf(key), enableUiOn
    ) {
        override fun createUi(context: Context): SwitchPreference =
            SwitchPreference(context).apply {
                key = this@Switch.key
                isIconSpaceReserved = false
                isSingleLineTitle = false
                setDefaultValue(defaultValue)
                if (this@Switch.summary != null)
                    setSummary(this@Switch.summary)
                setTitle(this@Switch.title)
            }
    }

    class StringList<T : Any>(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: T,
        val codec: ManagedPreference.StringLikeCodec<T>,
        val entries: List<Pair<String, T>>,
        enableUiOn: () -> Boolean = { true },
    ) : ManagedPreferenceUi<ListPreference>(setOf(key), enableUiOn) {
        override fun createUi(context: Context): ListPreference = ListPreference(context).apply {
            key = this@StringList.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            entryValues = this@StringList.entries.map { codec.encode(it.second) }.toTypedArray()
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setDefaultValue(codec.encode(defaultValue))
            setTitle(this@StringList.title)
            entries = this@StringList.entries.map { it.first }.toTypedArray()
            setDialogTitle(this@StringList.title)
        }
    }

    class SeekBarInt(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        enableUiOn: () -> Boolean = { true },
    ) : ManagedPreferenceUi<DialogSeekBarPreference>(setOf(key), enableUiOn) {
        override fun createUi(context: Context): DialogSeekBarPreference =
            DialogSeekBarPreference(context).apply {
                key = this@SeekBarInt.key
                isIconSpaceReserved = false
                isSingleLineTitle = false
                summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
                setDefaultValue(this@SeekBarInt.defaultValue)
                setTitle(this@SeekBarInt.title)
                min = this@SeekBarInt.min
                max = this@SeekBarInt.max
                unit = this@SeekBarInt.unit
            }

    }
}