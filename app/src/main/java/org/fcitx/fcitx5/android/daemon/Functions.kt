/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.daemon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.FcitxAPI

fun FcitxConnection.launchOnReady(block: suspend CoroutineScope.(FcitxAPI) -> Unit) {
    lifecycleScope.launch {
        runOnReady { block(this) }
    }
}