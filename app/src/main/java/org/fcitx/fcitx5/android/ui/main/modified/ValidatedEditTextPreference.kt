/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.modified

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import org.fcitx.fcitx5.android.R

class ValidatedEditTextPreference(context: Context) : EditTextPreference(context) {
    var validator: ((String) -> Boolean)? = null

    @StringRes
    var validationError: Int = R.string.invalid_value

    fun isValid(value: String) = validator?.invoke(value) != false
}
