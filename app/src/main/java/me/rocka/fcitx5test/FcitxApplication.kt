package me.rocka.fcitx5test

import android.app.Application
import androidx.preference.PreferenceManager
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.data.clipboard.ClipboardManager
import timber.log.Timber

class FcitxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "[${Thread.currentThread().name}] $tag", message, t)
            }
        })
        Prefs.init(PreferenceManager.getDefaultSharedPreferences(applicationContext))
        ClipboardManager.init(applicationContext)
    }

    companion object {
        private var instance: FcitxApplication? = null
        fun getInstance() =
            instance ?: throw IllegalStateException("Fcitx application is not created!")
    }
}