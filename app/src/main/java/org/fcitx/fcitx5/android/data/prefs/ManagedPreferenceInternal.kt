package org.fcitx.fcitx5.android.data.prefs

import android.content.SharedPreferences

abstract class ManagedPreferenceInternal(private val sharedPreferences: SharedPreferences) :
    ManagedPreferenceProvider {

    override val managedPreferences = mutableMapOf<String, ManagedPreference<*, *>>()

    protected fun int(key: String, defaultValue: Int) =
        ManagedPreference.RawInt(sharedPreferences, key, defaultValue).apply { register() }

    protected fun string(key: String, defaultValue: String) =
        ManagedPreference.RawString(sharedPreferences, key, defaultValue).apply { register() }

    protected fun <T : Any> stringLike(
        key: String,
        codec: ManagedPreference.StringLikeCodec<T>,
        defaultValue: T
    ) = ManagedPreference.RawStringLike(sharedPreferences, key, codec, defaultValue)
        .apply { register() }

    protected fun bool(key: String, defaultValue: Boolean) =
        ManagedPreference.RawBool(sharedPreferences, key, defaultValue).apply { register() }

    protected fun float(key: String, defaultValue: Float) =
        ManagedPreference.RawFloat(sharedPreferences, key, defaultValue).apply { register() }
}