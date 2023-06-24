package org.fcitx.fcitx5.android.data.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import org.fcitx.fcitx5.android.utils.WeakHashSet
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class ManagedPreference<T : Any>(
    val sharedPreferences: SharedPreferences,
    val key: String,
    val defaultValue: T,
) : ReadWriteProperty<Any?, T> {

    interface StringLikeCodec<T : Any> {
        fun encode(x: T): String = x.toString()
        fun decode(raw: String): T?
    }

    fun interface OnChangeListener<in T : Any> {
        fun onChange(key: String, value: T)
    }

    abstract fun setValue(value: T)

    abstract fun getValue(): T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getValue()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setValue(value)

    private val listeners by lazy {
        WeakHashSet<OnChangeListener<T>>()
    }

    /**
     * **WARN:** No anonymous listeners, please **KEEP** the reference!
     *
     * You may need to reference the listener once outside of it's container's constructor,
     * to prevent R8 from removing the field;
     * or simply mark the listener with [@Keep][androidx.annotation.Keep] .
     */
    fun registerOnChangeListener(listener: OnChangeListener<T>) {
        listeners.add(listener)
    }

    fun unregisterOnChangeListener(listener: OnChangeListener<T>) {
        listeners.remove(listener)
    }

    fun fireChange() {
        if (listeners.isEmpty()) return
        val newValue = getValue()
        listeners.forEach { it.onChange(key, newValue) }
    }

    class PBool(sharedPreferences: SharedPreferences, key: String, defaultValue: Boolean) :
        ManagedPreference<Boolean>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Boolean) {
            sharedPreferences.edit { putBoolean(key, value) }
        }

        override fun getValue(): Boolean = sharedPreferences.all[key]?.let {
            it.let { x -> x as? Boolean }
                ?: run {
                    setValue(defaultValue)
                    defaultValue
                }
        } ?: defaultValue
    }

    class PString(sharedPreferences: SharedPreferences, key: String, defaultValue: String) :
        ManagedPreference<String>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: String) {
            sharedPreferences.edit { putString(key, value) }
        }

        override fun getValue(): String = sharedPreferences.all[key]?.let {
            it.let { x -> x as? String }
                ?: run {
                    setValue(defaultValue)
                    defaultValue
                }
        } ?: defaultValue
    }

    class PStringLike<T : Any>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
        private val codec: StringLikeCodec<T>
    ) : ManagedPreference<T>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: T) {
            sharedPreferences.edit { putString(key, codec.encode(value)) }
        }

        override fun getValue(): T =
            sharedPreferences.all[key]?.let { raw ->
                if (raw !is String)
                    setValue(defaultValue)
                raw.let { it as? String }
                    ?.runCatching { codec.decode(this) }
                    ?.onFailure { Timber.w("Failed to decode value '$raw' of preference $key") }
                    ?.getOrNull()
            } ?: defaultValue
    }


    class PInt(sharedPreferences: SharedPreferences, key: String, defaultValue: Int) :
        ManagedPreference<Int>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Int) {
            sharedPreferences.edit { putInt(key, value) }
        }

        override fun getValue(): Int = sharedPreferences.all[key]?.let {
            it.let { x -> x as? Int }
                ?: run {
                    setValue(defaultValue)
                    defaultValue
                }
        } ?: defaultValue
    }

    class PFloat(sharedPreferences: SharedPreferences, key: String, defaultValue: Float) :
        ManagedPreference<Float>(sharedPreferences, key, defaultValue) {
        override fun setValue(value: Float) {
            sharedPreferences.edit { putFloat(key, value) }
        }

        override fun getValue(): Float = sharedPreferences.all[key]?.let {
            it.let { x -> x as? Float }
                ?: run {
                    setValue(defaultValue)
                    defaultValue
                }
        } ?: defaultValue
    }

}

