package me.rocka.fcitx5test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import me.rocka.fcitx5test.native.Fcitx
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FcitxDaemonManager {

    private val connections: HashMap<String, ServiceConnection> = hashMapOf()

    fun bindFcitxDaemonAsync(
        context: Context,
        name: String,
        onDisconnected: () -> Unit = {},
        onConnected: FcitxDaemon.FcitxBinder.() -> Unit
    ): ServiceConnection {
        if (connections.containsKey(name))
            return connections.getValue(name)
        else {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    onConnected(service as FcitxDaemon.FcitxBinder)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnected()
                }

            }
            context.bindService(
                Intent(context, FcitxDaemon::class.java),
                connection, Context.BIND_AUTO_CREATE
            )
            connections[name] = connection
            return connection
        }
    }

    suspend fun bindFcitxDaemon(context: Context, name: String): Fcitx =
        suspendCoroutine { cont ->
            bindFcitxDaemonAsync(context, name) { cont.resume(getFcitxInstance()) }
        }

    suspend fun bindFcitxDaemonReady(context: Context, name: String): Fcitx =
        suspendCoroutine { cont ->
            bindFcitxDaemonAsync(context, name) { onReady { cont.resume(getFcitxInstance()) } }
        }


    fun unbind(context: Context, name: String) {
        connections.remove(name)?.let {
            context.unbindService(it)
        }
    }

    fun unbindAll(context: Context) {
        connections.forEach { (_, connection) ->
            context.unbindService(connection)
        }
        connections.clear()
    }

    companion object {
        // for thread safety, this class shouldn't be used concurrently
        val instance by lazy { FcitxDaemonManager() }
    }
}