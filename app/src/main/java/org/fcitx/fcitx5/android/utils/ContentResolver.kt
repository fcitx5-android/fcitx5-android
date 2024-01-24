/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun ContentResolver.queryFileName(uri: Uri): String? = query(uri, null, null, null, null)?.use {
    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    it.moveToFirst()
    it.getString(index)
}
