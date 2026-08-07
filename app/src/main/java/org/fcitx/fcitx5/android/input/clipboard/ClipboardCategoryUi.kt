/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory

@get:StringRes
val ClipboardCategory.stringRes: Int
    get() = when (this) {
        ClipboardCategory.Phone -> R.string.clipboard_category_phone
        ClipboardCategory.Email -> R.string.clipboard_category_email
        ClipboardCategory.Url -> R.string.clipboard_category_url
        ClipboardCategory.Otp -> R.string.clipboard_category_otp
        ClipboardCategory.TrackingToken -> R.string.clipboard_category_tracking_token
        ClipboardCategory.Other -> R.string.clipboard_category_other
    }
