/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.core.data.PluginDescriptor
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber

object FcitxPluginServices {

    const val PLUGIN_SERVICE_ACTION = "${BuildConfig.APPLICATION_ID}.plugin.SERVICE"

    class PluginServiceConnection(
        private val pluginId: String,
        private val onDied: () -> Unit
    ) : ServiceConnection {
        private var messenger: Messenger? = null

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            messenger = Messenger(service)
            Timber.d("Plugin connected: $pluginId")
        }

        // may re-connect in the future
        override fun onServiceDisconnected(name: ComponentName) {
            messenger = null
            Timber.d("Plugin disconnected: $pluginId")
        }

        // will never receive another connection
        override fun onBindingDied(name: ComponentName?) {
            onDied.invoke()
            Timber.d("Plugin binding died: $pluginId")
        }

        fun sendMessage(message: Message) {
            try {
                messenger?.send(message)
            } catch (e: Throwable) {
                Timber.w("Cannot send message to plugin: $pluginId")
                Timber.w(e)
            }
        }
    }

    private val connections = mutableMapOf<String, PluginServiceConnection>()

    private fun connectPlugin(descriptor: PluginDescriptor) {
        val connection = PluginServiceConnection(descriptor.name) {
            disconnectPlugin(descriptor.name)
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

    fun sendMessage(message: Message) {
        connections.forEach { (_, conn) ->
            conn.sendMessage(message)
        }
    }
}