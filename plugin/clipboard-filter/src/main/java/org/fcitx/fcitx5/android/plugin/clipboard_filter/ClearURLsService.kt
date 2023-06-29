package org.fcitx.fcitx5.android.plugin.clipboard_filter

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import org.fcitx.fcitx5.android.common.ipc.FcitxRemoteConnection
import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer
import org.fcitx.fcitx5.android.common.ipc.bindFcitxRemoteService

class ClearURLsService : JobService() {

    private lateinit var connection: FcitxRemoteConnection

    private val transformer = object : IClipboardEntryTransformer.Stub() {
        override fun getPriority(): Int = 100

        override fun transform(clipboardText: String): String =
            ClearURLs.transform(clipboardText)

        override fun getDescription(): String = "ClearURLs"
    }

    override fun onCreate() {
        ClearURLs.initCatalog(assets)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        connection = bindFcitxRemoteService(BuildConfig.BUILD_TYPE == "debug") {
            Log.d("ClearURLsService", "bind to fcitx")
            it.registerClipboardEntryTransformer(transformer)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        runCatching {
            connection.remoteService?.unregisterClipboardEntryTransformer(transformer)
        }
        unbindService(connection)
        Log.d("ClearURLsService", "unbind from fcitx")
        return true
    }

}
