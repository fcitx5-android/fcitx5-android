/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.common.ipc

import android.os.RemoteException
import android.util.Log

fun IFcitxPluginIpcCallback.safeRespond(status: Int, msg: String, payload: ByteArray?) {
    try {
        respond(status, msg, payload)
    } catch (e: RemoteException) {
        Log.w("IFcitxPluginIpcCallback", "Failed to respond IPC callback", e)
    }
}