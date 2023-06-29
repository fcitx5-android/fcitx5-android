package org.fcitx.fcitx5.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer
import org.fcitx.fcitx5.android.common.ipc.IFcitxRemoteService
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.utils.Const
import timber.log.Timber
import java.util.PriorityQueue

class FcitxRemoteService : Service() {

    private val clipboardTransformerLock = Mutex()

    private val scope = MainScope() + CoroutineName("FcitxRemoteService")

    private val clipboardTransformers =
        PriorityQueue<IClipboardEntryTransformer>(3, compareByDescending { it.priority })

    private val IClipboardEntryTransformer.desc
        get() = runCatching { description }.getOrElse { FALLBACK_DESC }

    companion object {
        private const val FALLBACK_DESC = "x"
    }

    private suspend fun updateClipboardManager() = clipboardTransformerLock.withLock {
        ClipboardManager.transformer =
            if (clipboardTransformers.isEmpty()) null else ({ s: String ->
                var x = s
                clipboardTransformers.forEach {
                    x = runCatching { it.transform(x) }
                        .onFailure { it.printStackTrace() }
                        .getOrElse { x }
                }
                x
            }).also {
                Timber.d(
                    "Apply transformers ${
                        clipboardTransformers.joinToString { it.desc }
                    }"
                )
            }
    }

    private val binder = object : IFcitxRemoteService.Stub() {
        override fun getVersionName(): String = Const.versionName

        override fun getPid(): Int = Process.myPid()

        override fun getLoadedPlugins(): MutableMap<String, String> =
            DataManager.getLoadedPlugins().map {
                it.packageName to it.versionName
            }.let { mutableMapOf<String, String>().apply { putAll(it) } }

        override fun restartFcitx() {
            FcitxDaemon.restartFcitx()
        }

        override fun registerClipboardEntryTransformer(transformer: IClipboardEntryTransformer) {
            if (clipboardTransformers.any { runCatching { it.description }.getOrNull() == transformer.desc })
                return
            scope.launch {
                transformer.asBinder().linkToDeath({
                    unregisterClipboardEntryTransformer(transformer)
                }, 0)
                clipboardTransformers.add(transformer)
                Timber.d("registerClipboardEntryTransformer: ${transformer}[${transformer.desc}]")
                updateClipboardManager()
            }
        }

        override fun unregisterClipboardEntryTransformer(transformer: IClipboardEntryTransformer) {
            scope.launch {
                clipboardTransformers.remove(transformer)
                Timber.d("unregisterClipboardEntryTransformer: $transformer")
                updateClipboardManager()
            }
        }

    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("onBind: $intent")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("onUnbind: $intent")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        scope.cancel()
        clipboardTransformers.clear()
        runBlocking { updateClipboardManager() }
    }
}