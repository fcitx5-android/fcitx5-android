/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.common

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger

abstract class FcitxPluginService : Service() {

    private lateinit var messenger: Messenger

    open val handler: Handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent): IBinder {
        messenger = Messenger(handler)
        start()
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stop()
        return false
    }

    abstract fun start()

    abstract fun stop()
}