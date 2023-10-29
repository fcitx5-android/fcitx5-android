/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.plugin.clipboard_filter

import android.util.Log
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.ipc.FcitxRemoteConnection
import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer
import org.fcitx.fcitx5.android.common.ipc.bindFcitxRemoteService

class MainService : FcitxPluginService() {

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

    override fun start() {
        connection = bindFcitxRemoteService(BuildConfig.MAIN_APPLICATION_ID) {
            Log.d("ClearURLsService", "Bind to fcitx remote")
            it.registerClipboardEntryTransformer(transformer)
        }
    }

    override fun stop() {
        runCatching {
            connection.remoteService?.unregisterClipboardEntryTransformer(transformer)
        }
        unbindService(connection)
        Log.d("ClearURLsService", "Unbind from fcitx remote")
    }

}