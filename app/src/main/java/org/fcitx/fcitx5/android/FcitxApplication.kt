package org.fcitx.fcitx5.android

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.util.Log
import androidx.preference.PreferenceManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.ui.main.LogActivity
import timber.log.Timber
import kotlin.system.exitProcess

class FcitxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CaocConfig.Builder
            .create()
            .errorActivity(LogActivity::class.java)
            .enabled(!BuildConfig.DEBUG)
            .apply()
        instance = this
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (BuildConfig.DEBUG || sharedPrefs.getBoolean(applicationContext.getString(R.string.pref_verbose_log), false)) {
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    super.log(priority, "[${Thread.currentThread().name}] $tag", message, t)
                }
            })
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority == Log.VERBOSE || priority == Log.DEBUG) return
                    Log.println(priority, "[${Thread.currentThread().name}]", message)
                }
            })
        }

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Timber.i("Detected a locale change, process will exit now")
                exitProcess(0)
            }
        }, IntentFilter(Intent.ACTION_LOCALE_CHANGED))

        // record last pid for crash logs
        sharedPrefs.all["pid"]
            ?.let { it as? Int }
            ?.let {
                Timber.d("Last pid is $it")
                lastPid = it
            }
        val pid = Process.myPid()
        Timber.d("Set pid to current: $pid")
        sharedPrefs.edit().putInt("pid", pid).apply()

        Prefs.init(
            sharedPrefs,
            applicationContext.resources
        )
        ClipboardManager.init(applicationContext)
    }

    companion object {
        private var lastPid: Int? = null
        private var instance: FcitxApplication? = null
        fun getInstance() =
            instance ?: throw IllegalStateException("Fcitx application is not created!")

        fun getLastPid() = lastPid
    }
}