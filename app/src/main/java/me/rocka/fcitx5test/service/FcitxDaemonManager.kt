package me.rocka.fcitx5test.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import me.rocka.fcitx5test.utils.appContext
import java.lang.ref.WeakReference

object FcitxDaemonManager {

    private val connections:
            HashMap<String, Pair<WeakReference<Context>, FcitxDaemonConnection>> = hashMapOf()

    abstract class FcitxDaemonConnection : ServiceConnection {
        lateinit var service: FcitxDaemon.FcitxBinder
    }

    fun bindFcitxDaemon(
        name: String,
        context: Context = appContext,
        onDisconnected: () -> Unit = {},
        onConnected: FcitxDaemon.FcitxBinder.() -> Unit
    ): ServiceConnection = synchronized(connections) {
        if (connections.containsKey(name))
            return connections.getValue(name).second.also { onConnected(it.service) }
        else {
            val connection = object : FcitxDaemonConnection() {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    this.service = service as FcitxDaemon.FcitxBinder
                    onConnected(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnected()
                }

            }
            context.bindService(
                Intent(context, FcitxDaemon::class.java),
                connection, Context.BIND_AUTO_CREATE
            )
            connections[name] = WeakReference(context) to connection
            return connection
        }
    }

    fun unbind(name: String) = synchronized(connections) {
        connections.remove(name)?.let { (contextRef, connection) ->
            val context = contextRef.get()
            requireNotNull(context)
            context.unbindService(connection)
        }
    }

    fun unbindAll() = synchronized(connections) {
        connections.forEach { (_, entry) ->
            val (contextRef, connection) = entry
            val context = contextRef.get()
            requireNotNull(context)
            context.unbindService(connection)
        }
        connections.clear()
    }

    fun hasConnection(name: String) = name in connections

}