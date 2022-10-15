package org.fcitx.fcitx5.android.daemon

import org.fcitx.fcitx5.android.core.FcitxAPI

interface FcitxConnection {
    fun <T> runImmediately(block: suspend FcitxAPI.() -> T): T
    suspend fun <T> runOnReady(block: suspend FcitxAPI.() -> T): T
    suspend fun runIfReady(block: suspend FcitxAPI.() -> Unit)
}