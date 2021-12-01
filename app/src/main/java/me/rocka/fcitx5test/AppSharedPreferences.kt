package me.rocka.fcitx5test

import android.content.SharedPreferences
import androidx.core.content.edit
import me.rocka.fcitx5test.AppSharedPreferences.PreferenceKeys.AssetsVersion
import me.rocka.fcitx5test.AppSharedPreferences.PreferenceKeys.ButtonHapticFeedback
import me.rocka.fcitx5test.AppSharedPreferences.PreferenceKeys.HideKeyConfig
import me.rocka.fcitx5test.AppSharedPreferences.PreferenceKeys.IgnoreSystemCursor

class AppSharedPreferences(private val sharedPreferences: SharedPreferences) :
    SharedPreferences.OnSharedPreferenceChangeListener {

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        sharedPreferences.all.keys.forEach {
            onSharedPreferenceChanged(sharedPreferences, it)
        }
    }

    private var _assetsVersion: Long = -1L
    private var _ignoreSystemCursor: Boolean = true
    private var _hideKeyConfig: Boolean = true
    private var _buttonHapticFeedback: Boolean = true

    var assetsVersion
        get() = _assetsVersion
        set(value) {
            sharedPreferences.edit {
                putLong(AssetsVersion, value)
            }
        }
    var ignoreSystemCursor
        get() = _ignoreSystemCursor
        set(value) {
            sharedPreferences.edit {
                putBoolean(IgnoreSystemCursor, value)
            }
        }
    var hideKeyConfig
        get() = _hideKeyConfig
        set(value) {
            sharedPreferences.edit {
                putBoolean(HideKeyConfig, value)
            }
        }
    var buttonHapticFeedback
        get() = _buttonHapticFeedback
        set(value) {
            sharedPreferences.edit {
                putBoolean(ButtonHapticFeedback, value)
            }
        }

    object PreferenceKeys {
        const val AssetsVersion = "assets_version"
        const val IgnoreSystemCursor = "ignore_system_cursor"
        const val HideKeyConfig = "hide_key_config"
        const val ButtonHapticFeedback = "button_haptic_feedback"
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            AssetsVersion -> _assetsVersion = sharedPreferences.getLong(key, _assetsVersion)
            IgnoreSystemCursor -> _ignoreSystemCursor =
                sharedPreferences.getBoolean(key, _ignoreSystemCursor)
            HideKeyConfig -> _hideKeyConfig = sharedPreferences.getBoolean(key, _hideKeyConfig)
            ButtonHapticFeedback -> _buttonHapticFeedback =
                sharedPreferences.getBoolean(key, _buttonHapticFeedback)
        }

    }

    companion object {
        private var instance: AppSharedPreferences? = null

        /**
         * MUST call before use
         */
        @Synchronized
        fun init(sharedPreferences: SharedPreferences) {
            if (instance != null)
                throw IllegalStateException("Shared preference is already initialized!")
            instance = AppSharedPreferences(sharedPreferences)
        }

        @Synchronized
        fun getInstance() = instance!!
    }
}