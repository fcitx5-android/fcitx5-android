package org.fcitx.fcitx5.android.daemon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.FcitxAPI

fun FcitxConnection.launchOnReady(block: suspend CoroutineScope.(FcitxAPI) -> Unit) {
    lifecycleScope.launch {
        runOnReady { block(this) }
    }
}