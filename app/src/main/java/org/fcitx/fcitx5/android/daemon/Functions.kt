package org.fcitx.fcitx5.android.daemon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.FcitxAPI

fun CoroutineScope.launchOnFcitxReady(
    connection: FcitxConnection,
    block: suspend CoroutineScope.(FcitxAPI) -> Unit
) {
    launch {
        connection.runOnReady {
            this@launch.block(this)
        }
    }
}