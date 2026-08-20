/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.cloud_pinyin

import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginIpcCallback
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginService

class MainService : FcitxPluginService() {

    private val scope = MainScope() + CoroutineName("cloud_pinyin")

    private lateinit var impl: CloudPinyinImpl
    private lateinit var pluginService: IFcitxPluginService

    override fun onCreate() {
        impl = CloudPinyinImpl()
        pluginService = object : IFcitxPluginService.Stub() {
            override fun getPluginId() = "cloud_pinyin"

            override fun getClipboardEntryTransformerPriority() = -1
            override fun transformClipboardEntry(clipboardText: String?) = null

            override fun getCanHandleIpc() = true

            override fun onIpcNotify(method: String, params: ByteArray?) {
                if (params == null) return
                when (method) {
                    "set_backend" -> {
                        impl.setBackend(params[0].toInt())
                    }
                    "set_proxy" -> {
                        impl.setProxy(String(params))
                    }
                }
            }

            override fun onIpcRequest(
                method: String,
                params: ByteArray?,
                cb: IFcitxPluginIpcCallback
            ) {
                when (method) {
                    "request" -> scope.launch {
                        if (params != null) {
                            val result = impl.request(String(params))
                            cb.respond(0, result, null)
                        } else {
                            cb.respond(2, "null params", null)
                        }
                    }
                    else -> {
                        cb.respond(1, "unsupported", null)
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