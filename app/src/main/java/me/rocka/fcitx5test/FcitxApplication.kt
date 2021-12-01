package me.rocka.fcitx5test

import android.app.Application
import androidx.preference.PreferenceManager

class FcitxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSharedPreferences.init(PreferenceManager.getDefaultSharedPreferences(applicationContext))
    }
}