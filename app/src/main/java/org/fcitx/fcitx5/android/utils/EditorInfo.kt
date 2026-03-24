/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * When the EditorInfo has [InputType.TYPE_NULL] and [EditorInfo.IME_NULL], it's likely not an input field
 */
fun EditorInfo.isTypeNull(): Boolean {
    return inputType == InputType.TYPE_NULL && imeOptions == EditorInfo.IME_NULL
}
