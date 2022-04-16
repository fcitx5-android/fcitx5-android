package org.fcitx.fcitx5.android.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import org.fcitx.fcitx5.android.ui.common.DialogSeekBarPreference
import org.fcitx.fcitx5.android.utils.WeakHashSet
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class ManagedPreference<T : Any, P : Preference>(
    val sharedPreferences: SharedPreferences,
    val key: String,
    val defaultValue: T,
    private val uiConfig: P.() -> Unit
) : ReadWriteProperty<Any?, T> {

    fun interface OnChangeListener<T : Any> {
        fun ManagedPreference<T, *>.onChange()
    }

    private val listeners by lazy { WeakHashSet<OnChangeListener<T>>() }

    fun createUi(context: Context): P = createUiProtected(context).apply(uiConfig)

    protected abstract fun createUiProtected(context: Context): P

    abstract fun setValue(value: T)

    abstract fun getValue(): T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getValue()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setValue(value)

    /**
     * **WARN:** No anonymous listeners, please **KEEP** the reference!
     */
    fun registerOnChangeListener(listener: OnChangeListener<T>) {
        listeners.add(listener)
    }

    fun unregisterOnChangeListener(listener: OnChangeListener<T>) {
        listeners.remove(listener)
    }

    fun fireChange() {
        listeners.forEach { with(it) { onChange() } }
    }

    interface StringLikeCodec<T : Any> {
        fun encode(x: T): String = x.toString()
        fun decode(raw: String): T?
    }

    class Switch(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Boolean,
        uiConfig: SwitchPreference.() -> Unit
    ) : ManagedPreference<Boolean, SwitchPreference>(
        sharedPreferences,
        key,
        defaultValue,
        uiConfig
    ) {
        override fun createUiProtected(context: Context): SwitchPreference =
            SwitchPreference(context).apply {
                key = this@Switch.key
                isIconSpaceReserved = false
                isSingleLineTitle = false
                setDefaultValue(defaultValue)
            }

        override fun setValue(value: Boolean) {
            sharedPreferences.edit { putBoolean(key, value) }
        }

        override fun getValue(): Boolean = sharedPreferences.getBoolean(key, defaultValue)

    }

    class StringList(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: String,
        val entries: Array<String>,
        uiConfig: ListPreference.() -> Unit
    ) : ManagedPreference<String, ListPreference>(sharedPreferences, key, defaultValue, uiConfig) {

        override fun createUiProtected(context: Context): ListPreference =
            ListPreference(context).apply {
                key = this@StringList.key
                isIconSpaceReserved = false
                isSingleLineTitle = false
                entryValues = this@StringList.entries
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                setDefaultValue(defaultValue)
            }

        override fun setValue(value: String) {
            sharedPreferences.edit { putString(key, value) }
        }

        override fun getValue(): String = sharedPreferences.getString(key, defaultValue)!!
    }

    class StringLikeList<T : Any>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
        val codec: StringLikeCodec<T>,
        val entries: List<T>,
        uiConfig: ListPreference.() -> Unit
    ) : ManagedPreference<T, ListPreference>(sharedPreferences, key, defaultValue, uiConfig) {
        override fun createUiProtected(context: Context): ListPreference =
            ListPreference(context).apply {
                key = this@StringLikeList.key
                isIconSpaceReserved = false
                isSingleLineTitle = false
                entryValues = this@StringLikeList.entries.map { codec.encode(it) }.toTypedArray()
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                setDefaultValue(codec.encode(defaultValue))
            }

        override fun setValue(value: T) {
            sharedPreferences.edit { putString(key, codec.encode(value)) }
        }

        override fun getValue(): T =
            sharedPreferences.getString(key, null).let { raw ->
                raw?.runCatching { codec.decode(this) }
                    ?.onFailure { Timber.w("Failed to decode value '$raw' of preference $key") }
                    ?.getOrNull() ?: defaultValue
            }
    }

    class SeekBarInt(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Int,
        uiConfig: DialogSeekBarPreference.() -> Unit
    ) : ManagedPreference<Int, DialogSeekBarPreference>(
        sharedPreferences,
        key,
        defaultValue,
        uiConfig
    ) {
        override fun createUiProtected(context: Context): DialogSeekBarPreference =
            DialogSeekBarPreference(context).apply {
                key = this@SeekBarInt.key
                isIconSpaceReserved = false
                isSingleLineTitle = false
                summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
                defaultValue = this@SeekBarInt.defaultValue
            }

        override fun setValue(value: Int) = sharedPreferences.edit { putInt(key, value) }

        override fun getValue(): Int = sharedPreferences.getInt(key, defaultValue)

    }

    abstract class NoUiManagedPreference<T : Any>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T
    ) : ManagedPreference<T, Nothing>(sharedPreferences, key, defaultValue, {}) {
        final override fun createUiProtected(context: Context): Nothing =
            throw UnsupportedOperationException()
    }

    class RawBool(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Boolean,
    ) : NoUiManagedPreference<Boolean>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Boolean) {
            sharedPreferences.edit { putBoolean(key, value) }
        }

        override fun getValue(): Boolean = sharedPreferences.getBoolean(key, defaultValue)
    }

    class RawString(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: String,
    ) : NoUiManagedPreference<String>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: String) {
            sharedPreferences.edit { putString(key, value) }

        }

        override fun getValue(): String = sharedPreferences.getString(key, defaultValue)!!
    }

    class RawStringLike<T : Any>(
        sharedPreferences: SharedPreferences,
        key: String,
        val codec: StringLikeCodec<T>,
        defaultValue: T,
    ) : NoUiManagedPreference<T>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: T) {
            sharedPreferences.edit { putString(key, codec.encode(value)) }
        }

        override fun getValue(): T = sharedPreferences.getString(key, null).let { raw ->
            raw?.let { codec.decode(it) }
                ?: throw RuntimeException("Failed to decode preference [$key] $raw")
        }
    }

    class RawInt(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Int,
    ) : NoUiManagedPreference<Int>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Int) {
            sharedPreferences.edit { putInt(key, value) }
        }

        override fun getValue(): Int = sharedPreferences.getInt(key, defaultValue)
    }

    class RawFloat(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Float,
    ) : NoUiManagedPreference<Float>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Float) {
            sharedPreferences.edit { putFloat(key, value) }
        }

        override fun getValue(): Float = sharedPreferences.getFloat(key, defaultValue)
    }

}

