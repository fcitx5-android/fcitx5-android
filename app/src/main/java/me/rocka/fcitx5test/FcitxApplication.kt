package me.rocka.fcitx5test

import android.app.Application
import androidx.preference.PreferenceManager

class FcitxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AppSharedPreferences.init(PreferenceManager.getDefaultSharedPreferences(applicationContext))
    }

    companion object {
        private var instance: FcitxApplication? = null
        fun getInstance() =
            instance ?: throw IllegalStateException("Fcitx application is not created!")
    }
}