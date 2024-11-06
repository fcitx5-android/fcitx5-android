/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

@file:Suppress("DEPRECATION")

package org.fcitx.fcitx5.android.utils

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.inputmethod.InputMethodManager

fun InputMethodService.forceShowSelf() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        requestShowSelf(InputMethodManager.SHOW_FORCED)
    } else {
        inputMethodManager.showSoftInputFromInputMethod(
            window.window!!.attributes.token,
            InputMethodManager.SHOW_FORCED
        )
    }
}

fun InputMethodService.switchToNextIME() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        switchToNextInputMethod(false)
    } else {
        inputMethodManager.switchToNextInputMethod(window.window!!.attributes.token, false)
    }
}
