/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
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
import org.fcitx.fcitx5.android.core.reloadPinyinDict
import org.fcitx.fcitx5.android.core.reloadQuickPhrase
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.utils.Const
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class FcitxRemoteService : Service() {

    private val clipboardTransformerLock = Mutex()

    private val scope = MainScope() + CoroutineName("FcitxRemoteService")

    private class ClipTransformer(
        val priority: Int,
        val desc: String,
        val service: IClipboardEntryTransformer
    ) {
        override fun toString() = "$priority:$desc"
    }

    private val clipboardTransformers = CopyOnWriteArrayList<ClipTransformer>()

    private fun transformClipboard(source: String): String {
        var result = source
        clipboardTransformers.forEach {
            try {
                result = it.service.transform(result)!!
            } catch (e: Exception) {
                Timber.w("Exception while calling clipboard transformer '$it'")
                Timber.w(e)
            }
        }
        return result
    }

    private suspend fun updateClipboardManager() = clipboardTransformerLock.withLock {
        ClipboardManager.transformer =
            if (clipboardTransformers.isEmpty()) null else ::transformClipboard
        Timber.d("All clipboard transformers: ${clipboardTransformers.joinToString()}")
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
            val t = ClipTransformer(transformer.priority, transformer.description, transformer)
            Timber.d("registerClipboardEntryTransformer: $t")
            if (t.desc.isBlank()) {
                Timber.w("Cannot register ClipboardEntryTransformer of null or empty description")
                return
            }
            if (clipboardTransformers.any { it.desc == t.desc }) {
                Timber.w("ClipboardEntryTransformer '${t.desc}' has already been registered")
                return
            }
            scope.launch {
                transformer.asBinder().linkToDeath({
                    clipboardTransformers.removeIf { it.desc == t.desc }
                }, 0)
                clipboardTransformers.add(t)
                clipboardTransformers.sortByDescending { it.priority }
                updateClipboardManager()
            }
        }

        override fun unregisterClipboardEntryTransformer(transformer: IClipboardEntryTransformer) {
            val desc = transformer.description
            Timber.d("unregisterClipboardEntryTransformer: $desc")
            scope.launch {
                clipboardTransformers.removeIf { it.desc == desc }
                updateClipboardManager()
            }
        }

        override fun reloadPinyinDict() {
            FcitxDaemon.getFirstConnectionOrNull()?.runIfReady { reloadPinyinDict() }
        }

        override fun reloadQuickPhrase() {
            FcitxDaemon.getFirstConnectionOrNull()?.runIfReady { reloadQuickPhrase() }
        }
    }

    override fun onCreate() {
        Timber.d("FcitxRemoteService onCreate")
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("FcitxRemoteService onBind: $intent")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("FcitxRemoteService onUnbind: $intent")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Timber.d("FcitxRemoteService onDestroy")
        scope.cancel()
        clipboardTransformers.clear()
        runBlocking { updateClipboardManager() }
    }
}