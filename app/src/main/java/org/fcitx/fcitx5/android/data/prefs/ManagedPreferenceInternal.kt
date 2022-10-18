package org.fcitx.fcitx5.android.data.prefs

import android.content.SharedPreferences

abstract class ManagedPreferenceInternal(private val sharedPreferences: SharedPreferences) :
    ManagedPreferenceProvider() {

    protected fun int(key: String, defaultValue: Int) =
        ManagedPreference.PInt(sharedPreferences, key, defaultValue).apply { register() }

    protected fun string(key: String, defaultValue: String) =
        ManagedPreference.PString(sharedPreferences, key, defaultValue).apply { register() }

    protected fun <T : Any> stringLike(
        key: String,
        codec: ManagedPreference.StringLikeCodec<T>,
        defaultValue: T
    ) = ManagedPreference.PStringLike(sharedPreferences, key, defaultValue, codec)
        .apply { register() }

    protected fun bool(key: String, defaultValue: Boolean) =
        ManagedPreference.PBool(sharedPreferences, key, defaultValue).apply { register() }

    protected fun float(key: String, defaultValue: Float) =
        ManagedPreference.PFloat(sharedPreferences, key, defaultValue).apply { register() }
}