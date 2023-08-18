package org.fcitx.fcitx5.android.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber

object FcitxPluginServices {
    class PluginServiceConnection(private val pluginId: String) : ServiceConnection {
        private var messenger: Messenger? = null
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            messenger = Messenger(service)
            Timber.d("$pluginId connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            messenger = null
            Timber.d("$pluginId disconnected")
        }

        fun sendMessage(message: Message) {
            runCatching {
                messenger?.send(message)
            }.onFailure { it.printStackTrace() }
        }

    }

    private val connections = mutableMapOf<String, PluginServiceConnection>()

    fun connectAll() {
        DataManager.getLoadedPlugins().forEach { descriptor ->
            if (descriptor.name in connections)
                return@forEach
            runCatching {
                val connection = PluginServiceConnection(descriptor.name)
                appContext.bindService(
                    Intent("${descriptor.packageName}.service").also { it.setPackage(descriptor.packageName) },
                    connection,
                    Context.BIND_AUTO_CREATE
                )
            }.getOrNull()?.takeIf { it }?.run {
                Timber.d("Bind to ${descriptor.name}'s service")
            }
        }
    }

    fun disconnectAll() {
        connections.forEach { (id, connection) ->
            appContext.unbindService(connection)
            Timber.d("Unbound $id")
        }
    }

    fun sendMessage(message: Message) {
        connections.forEach { (_, conn) ->
            conn.sendMessage(message)
        }
    }
}