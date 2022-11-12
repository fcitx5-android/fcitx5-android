package org.fcitx.fcitx5.android

import android.app.Application
import android.content.res.Configuration
import android.os.Process
import android.util.Log
import androidx.preference.PreferenceManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.main.LogActivity
import org.fcitx.fcitx5.android.utils.isDarkMode
import timber.log.Timber

class FcitxApplication : Application() {

    val openTelemetry: OpenTelemetrySdk = OpenTelemetrySdk
        .builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(
                    Resource.create(
                        Attributes.of(
                            AttributeKey.stringKey("service.name"),
                            "fcitx5-android"
                        )
                    )
                )
                .setSampler(Sampler.alwaysOff())
                .addSpanProcessor(BatchSpanProcessor.builder(LoggingSpanExporter.create()).build())
                .build()
        )
        .buildAndRegisterGlobal()

    override fun onCreate() {
        super.onCreate()
        CaocConfig.Builder.create()
            .enabled(!BuildConfig.DEBUG)
            .errorActivity(LogActivity::class.java)
            .apply()
        instance = this
        // we don't have AppPrefs available yet
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (BuildConfig.DEBUG || sharedPrefs.getBoolean("verbose_log", false)) {
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    super.log(priority, "[${Thread.currentThread().name}] $tag", message, t)
                }
            })
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority < Log.INFO) return
                    Log.println(priority, "[${Thread.currentThread().name}]", message)
                }
            })
        }

        AppPrefs.init(sharedPrefs)
        // record last pid for crash logs
        AppPrefs.getInstance().internal.pid.apply {
            val currentPid = Process.myPid()
            lastPid = getValue()
            Timber.d("Last pid is $lastPid. Set it to current pid: $currentPid")
            setValue(currentPid)
        }
        ClipboardManager.init(applicationContext)
        ThemeManager.init(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ThemeManager.onSystemDarkModeChanged(newConfig.isDarkMode())
    }

    companion object {
        private var lastPid: Int? = null
        private var instance: FcitxApplication? = null
        fun getInstance() =
            instance ?: throw IllegalStateException("Fcitx application is not created!")

        fun getLastPid() = lastPid
    }
}