/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.common

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

abstract class FcitxPluginService : Service() {
    override fun onBind(intent: Intent): IBinder {
        start()
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stop()
        return false
    }

    abstract fun start()

    abstract fun stop()
}