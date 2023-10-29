/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
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
import org.fcitx.fcitx5.android.utils.desc
import org.fcitx.fcitx5.android.utils.descEquals
import timber.log.Timber
import java.util.PriorityQueue

class FcitxRemoteService : Service() {

    private val clipboardTransformerLock = Mutex()

    private val scope = MainScope() + CoroutineName("FcitxRemoteService")

    private val clipboardTransformers =
        PriorityQueue<IClipboardEntryTransformer>(3, compareByDescending { it.priority })

    private fun transformClipboard(source: String): String {
        MainScope()
        var result = source
        clipboardTransformers.forEach {
            try {
                result = it.transform(result)!!
            } catch (e: Exception) {
                Timber.w("Exception while calling clipboard transformer '${it.desc}'")
                Timber.w(e)
            }
        }
        return result
    }

    private suspend fun updateClipboardManager() = clipboardTransformerLock.withLock {
        ClipboardManager.transformer =
            if (clipboardTransformers.isEmpty()) null else ::transformClipboard
        Timber.d("All clipboard transformers: ${clipboardTransformers.joinToString { it.desc }}")
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
            Timber.d("registerClipboardEntryTransformer: ${transformer.desc}")
            try {
                transformer.description!!.isNotEmpty() || throw Exception()
            } catch (e: Exception) {
                Timber.w("Cannot register ClipboardEntryTransformer of null or empty description")
                return
            }
            if (clipboardTransformers.any { it.descEquals(transformer) }) {
                Timber.w("ClipboardEntryTransformer ${transformer.desc} has already been registered")
                return
            }
            scope.launch {
                transformer.asBinder().linkToDeath({
                    unregisterClipboardEntryTransformer(transformer)
                }, 0)
                clipboardTransformers.add(transformer)
                updateClipboardManager()
            }
        }

        override fun unregisterClipboardEntryTransformer(transformer: IClipboardEntryTransformer) {
            Timber.d("unregisterClipboardEntryTransformer: ${transformer.desc}")
            scope.launch {
                clipboardTransformers.remove(transformer)
                        || clipboardTransformers.removeIf { it.descEquals(transformer) }
                        || return@launch
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