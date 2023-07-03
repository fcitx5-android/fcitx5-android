package org.fcitx.fcitx5.android.utils

import android.content.Context
import android.content.Intent
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.common.Broadcasts

object Broadcaster {
    private val broadcasts by lazy { Broadcasts(BuildConfig.APPLICATION_ID) }
    fun broadcast(context: Context, action: (Broadcasts) -> String) =
        context.sendBroadcast(Intent(action(broadcasts)), broadcasts.PERMISSION)
}