/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.clipboard_filter

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginIpcCallback
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginService

class MainService : Service() {
    companion object {
        const val TAG = "clipboard_filter"
    }

    private lateinit var pluginService: IFcitxPluginService

    override fun onCreate() {
        ClearURLs.initCatalog(assets.open("data.min.json").bufferedReader().readText())
        pluginService = object : IFcitxPluginService.Stub() {
            override fun getPluginId() = TAG

            override fun getClipboardEntryTransformerPriority() = 100
            override fun transformClipboardEntry(clipboardText: String?): String {
                if (clipboardText == null) return ""
                return ClearURLs.transform(clipboardText)
            }

            override fun getCanHandleIpc() = false
            override fun onIpcNotify(method: String?, params: ByteArray?) {}
            override fun onIpcRequest(
                method: String?,
                params: ByteArray?,
                cb: IFcitxPluginIpcCallback?
            ) {
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return pluginService.asBinder()
    }
}