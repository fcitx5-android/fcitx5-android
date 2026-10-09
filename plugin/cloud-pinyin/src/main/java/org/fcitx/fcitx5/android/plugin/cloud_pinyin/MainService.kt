/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.cloud_pinyin

import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginIpcCallback
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginService

class MainService : FcitxPluginService() {
    companion object {
        const val TAG = "cloud_pinyin"
    }

    private val scope = MainScope() + CoroutineName(TAG)

    private lateinit var impl: CloudPinyinImpl
    private lateinit var pluginService: IFcitxPluginService

    override fun onCreate() {
        impl = CloudPinyinImpl()
        pluginService = object : IFcitxPluginService.Stub() {
            override fun getPluginId() = TAG

            override fun getClipboardEntryTransformerPriority() = -1
            override fun transformClipboardEntry(clipboardText: String?) = null

            override fun getCanHandleIpc() = true

            override fun onIpcNotify(method: String, params: ByteArray?) {
            }

            @OptIn(ExperimentalSerializationApi::class)
            override fun onIpcRequest(
                method: String,
                params: ByteArray?,
                cb: IFcitxPluginIpcCallback
            ) {
                when (method) {
                    "request" -> scope.launch {
                        try {
                            val args = Cbor.decodeFromByteArray<CloudPinyinPayload>(params!!)
                            val result = impl.request(args)
                            cb.respond(0, result, null)
                        } catch (e: CancellationException) {
                            // allow coroutine cancel
                            throw e
                        } catch(e: Exception) {
                            // there will be no stack trace if it's an UnknownHostException because of network unavailable
                            // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/util/Log.java#486
                            Log.w(TAG, "Exception when requesting", e)
                            cb.respond(1, "failed", null)
                        }
                    }.invokeOnCompletion {  }
                    else -> {
                        Log.w(TAG, "Unsupported request: $method")
                        cb.respond(2, "unsupported", null)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return pluginService.asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return false
    }

    override fun start() {
    }

    override fun stop() {
    }
}