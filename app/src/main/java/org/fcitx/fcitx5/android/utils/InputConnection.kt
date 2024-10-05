/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.os.Build
import android.view.inputmethod.InputConnection

fun InputConnection.withBatchEdit(block: InputConnection.() -> Unit) {
    beginBatchEdit()
    block.invoke(this)
    endBatchEdit()
}

fun InputConnection.monitorCursorAnchor(enable: Boolean = true): Boolean {
    if (!enable) {
        requestCursorUpdates(0)
        return false
    }
    var scheduled = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        scheduled = requestCursorUpdates(
            InputConnection.CURSOR_UPDATE_MONITOR,
            InputConnection.CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS or InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER
        )
    }
    if (!scheduled) {
        scheduled = requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
    }
    return scheduled
}
