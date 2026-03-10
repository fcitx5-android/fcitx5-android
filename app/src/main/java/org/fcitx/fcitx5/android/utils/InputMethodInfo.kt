/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodSubtype

fun InputMethodInfo.firstVoiceSubtype() : InputMethodSubtype? {
    for (index in 0 until subtypeCount) {
        val subtype = getSubtypeAt(index)
        if (subtype.mode.lowercase() == "voice") {
            return subtype
        }
    }
    return null
}
