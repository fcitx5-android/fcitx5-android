package org.fcitx.fcitx5.android.common.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

fun Context.bindFcitxRemoteService(
    debugBuild: Boolean,
    onDisconnect: () -> Unit = {},
    onConnected: (IFcitxRemoteService) -> Unit
): FcitxRemoteConnection {
    val connection = FcitxRemoteConnection(onConnected, onDisconnect)
    bindService(
        Intent("org.fcitx.fcitx5.android.IPC").apply {
            val pkgName = "org.fcitx.fcitx5.android" + if (debugBuild) ".debug" else ""
            setPackage(pkgName)
        },
        connection,
        Context.BIND_AUTO_CREATE
    )
    return connection
}

open class FcitxRemoteConnection(
    private val onConnected: (IFcitxRemoteService) -> Unit,
    private val onDisconnected: () -> Unit
) : ServiceConnection {
    var remoteService: IFcitxRemoteService? = null
        private set

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        IFcitxRemoteService.Stub.asInterface(service).let {
            remoteService = it
            onConnected(it)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        onDisconnected()
    }

}