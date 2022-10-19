package org.fcitx.fcitx5.android.data.prefs

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import org.fcitx.fcitx5.android.ui.main.settings.DialogSeekBarPreference
import org.fcitx.fcitx5.android.ui.main.settings.TwinSeekBarPreference

abstract class ManagedPreferenceUi<T : Preference>(
    val key: String,
    val enableUiOn: () -> Boolean
) {

    abstract fun createUi(context: Context): T

    class Switch(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Boolean,
        @StringRes
        val summary: Int? = null,
        enableUiOn: () -> Boolean = { true }
    ) : ManagedPreferenceUi<SwitchPreference>(key, enableUiOn) {
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
    ) : ManagedPreferenceUi<ListPreference>(key, enableUiOn) {
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
    ) : ManagedPreferenceUi<DialogSeekBarPreference>(key, enableUiOn) {
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

    class TwinSeekBarInt(
        @StringRes
        val title: Int,
        @StringRes
        val label: Int,
        key: String,
        val defaultValue: Int,
        @StringRes
        val secondaryLabel: Int,
        val secondaryKey: String,
        val secondaryDefaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        enableUiOn: () -> Boolean = { true },
    ) : ManagedPreferenceUi<TwinSeekBarPreference>(key, enableUiOn) {
        override fun createUi(context: Context): TwinSeekBarPreference =
            TwinSeekBarPreference(context).apply {
                setTitle(this@TwinSeekBarInt.title)
                label = context.getString(this@TwinSeekBarInt.label)
                key = this@TwinSeekBarInt.key
                secondaryLabel = context.getString(this@TwinSeekBarInt.secondaryLabel)
                secondaryKey = this@TwinSeekBarInt.secondaryKey
                setDefaultValue(this@TwinSeekBarInt.defaultValue to this@TwinSeekBarInt.secondaryDefaultValue)
                min = this@TwinSeekBarInt.min
                max = this@TwinSeekBarInt.max
                unit = this@TwinSeekBarInt.unit
                isIconSpaceReserved = false
                isSingleLineTitle = false
                summaryProvider = TwinSeekBarPreference.SimpleSummaryProvider
            }
    }
}