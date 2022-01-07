package me.rocka.fcitx5test.content

import android.content.SharedPreferences
import androidx.core.content.edit
import me.rocka.fcitx5test.content.AppSharedPreferences.PreferenceKeys.ButtonHapticFeedback
import me.rocka.fcitx5test.content.AppSharedPreferences.PreferenceKeys.FirstRun
import me.rocka.fcitx5test.content.AppSharedPreferences.PreferenceKeys.HideKeyConfig
import me.rocka.fcitx5test.content.AppSharedPreferences.PreferenceKeys.IgnoreSystemCursor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AppSharedPreferences(private val sharedPreferences: SharedPreferences) {

    private inline fun <reified T> preference(key: String, defaultValue: T) =
        object : ReadWriteProperty<AppSharedPreferences, T> {
            override fun getValue(thisRef: AppSharedPreferences, property: KProperty<*>): T {
                return (thisRef.sharedPreferences.all[key] ?: defaultValue) as T
            }

            override fun setValue(thisRef: AppSharedPreferences, property: KProperty<*>, value: T) {
                when (value) {
                    is Boolean -> thisRef.sharedPreferences.edit { putBoolean(key, value) }
                    is Long -> thisRef.sharedPreferences.edit { putLong(key, value) }
                    is Float -> thisRef.sharedPreferences.edit { putFloat(key, value) }
                    is Int -> thisRef.sharedPreferences.edit { putInt(key, value) }
                    is String -> thisRef.sharedPreferences.edit { putString(key, value) }
                }
            }

        }


    var firstRun by preference(FirstRun, true)
    var ignoreSystemCursor by preference(IgnoreSystemCursor, true)
    var hideKeyConfig by preference(HideKeyConfig, true)
    var buttonHapticFeedback by preference(ButtonHapticFeedback, true)

    object PreferenceKeys {
        const val FirstRun = "first_run"
        const val IgnoreSystemCursor = "ignore_system_cursor"
        const val HideKeyConfig = "hide_key_config"
        const val ButtonHapticFeedback = "button_haptic_feedback"
    }


    companion object {
        private var instance: AppSharedPreferences? = null

        /**
         * MUST call before use
         */
        @Synchronized
        fun init(sharedPreferences: SharedPreferences) {
            if (instance != null)
                return
            instance = AppSharedPreferences(sharedPreferences)
        }

        @Synchronized
        fun getInstance() = instance!!
    }
}