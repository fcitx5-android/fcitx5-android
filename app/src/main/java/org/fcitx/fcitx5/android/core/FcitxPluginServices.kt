/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginIpcCallback
import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginService
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.core.data.PluginDescriptor
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber

object FcitxPluginServices {

    const val PLUGIN_SERVICE_ACTION = "${BuildConfig.APPLICATION_ID}.plugin.SERVICE"

    interface ClipboardTransformer {
        val priority: Int
        fun transform(text: String): String
    }

    private var legacyClipTransformers: List<ClipboardTransformer>? = null

    fun updateLegacyClipTransformers(list: List<ClipboardTransformer>?) {
        legacyClipTransformers = list
    }

    class PluginServiceConnection(
        val packageName: String,
        private val onDied: PluginServiceConnection.() -> Unit
    ) : ServiceConnection {
        private var service: IFcitxPluginService? = null

        var pluginId: String? = null
            private set

        var clipboardTransformerPriority = -1
            private set

        var canHandleIpc = false
            private set

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("Plugin connected: $packageName")
            val pluginService = IFcitxPluginService.Stub.asInterface(service)
            try {
                this.pluginId = pluginService.pluginId
                this.clipboardTransformerPriority = pluginService.clipboardEntryTransformerPriority
                this.canHandleIpc = pluginService.canHandleIpc
                this.service = pluginService
            } catch (e: Exception) {
                Timber.w("Unable to bind plugin service $name: ${e.message}")
            }
        }

        // may re-connect in the future
        override fun onServiceDisconnected(name: ComponentName) {
            Timber.d("Plugin disconnected: $packageName")
        }

        // will never receive another connection
        override fun onBindingDied(name: ComponentName?) {
            onDied.invoke(this)
            Timber.d("Plugin binding died: $packageName")
        }

        fun handleIpc(method: String, params: ByteArray?, cb: IFcitxPluginIpcCallback?): Boolean {
            val s = service ?: return false
            try {
                if (cb == null) {
                    s.onIpcNotify(method, params)
                } else {
                    s.onIpcRequest(method, params, cb)
                }
                return true
            } catch (e: Exception) {
                Timber.w("Exception when calling plugin IPC $pluginId.$method: ${e.message}")
                return false
            }
        }

        val clipboardTransformer: ClipboardTransformer? =
            if (clipboardTransformerPriority < 0) null
            else object : ClipboardTransformer {
                override val priority = clipboardTransformerPriority
                override fun transform(text: String): String {
                    val s = service ?: return text
                    try {
                        return s.transformClipboardEntry(text)
                    } catch (e: Exception) {
                        Timber.w("Exception when transformClipboardEntry with plugin $pluginId: ${e.message}")
                        return text
                    }
                }
            }
    }

    private val connections = mutableMapOf<String, PluginServiceConnection>()

    private fun connectPlugin(descriptor: PluginDescriptor) {
        val connection = PluginServiceConnection(descriptor.packageName) {
            disconnectPlugin(packageName)
        }
        try {
            val result = appContext.bindService(
                Intent(PLUGIN_SERVICE_ACTION).apply { setPackage(descriptor.packageName) },
                connection,
                Context.BIND_AUTO_CREATE
            )
            if (!result) throw Exception("Couldn't find service or not enough permission")
            connections[descriptor.name] = connection
            Timber.d("Bind to plugin: ${descriptor.name}")
        } catch (e: Exception) {
            appContext.unbindService(connection)
            Timber.w("Cannot bind to plugin: ${descriptor.name}")
            Timber.w(e)
        }
    }

    fun connectAll() {
        DataManager.getLoadedPlugins().forEach {
            if (it.hasService && !connections.containsKey(it.name)) {
                connectPlugin(it)
            }
        }
    }

    private fun disconnectPlugin(name: String) {
        connections.remove(name)?.also {
            appContext.unbindService(it)
            Timber.d("Unbound plugin: $name")
        }
    }

    fun disconnectAll() {
        connections.forEach { (name, connection) ->
            appContext.unbindService(connection)
            Timber.d("Unbound plugin: $name")
        }
        connections.clear()
    }

    fun transformClipboardEntry(clipboardText: String): String {
        val transformers: List<ClipboardTransformer> = buildList {
            connections.values.forEach { it.clipboardTransformer?.let(::add) }
            legacyClipTransformers?.let(::addAll)
        }
        return transformers
            .sortedByDescending { it.priority }
            .fold(clipboardText) { text, it -> it.transform(text) }
    }

    fun handlePluginIpc(id: Int, plugin: String, method: String, params: ByteArray?): Boolean {
        val target = connections.values.find { it.pluginId == plugin } ?: return false
        val cb = if (id < 0) null else object : IFcitxPluginIpcCallback.Stub() {
            override fun respond(status: Int, msg: String, payload: ByteArray?) {
                FcitxDaemon.getFirstConnectionOrNull()?.runIfReady {
                    respondIpcRequest(id, status, msg, payload)
                }
            }
        }
        return target.handleIpc(method, params, cb)
    }
}