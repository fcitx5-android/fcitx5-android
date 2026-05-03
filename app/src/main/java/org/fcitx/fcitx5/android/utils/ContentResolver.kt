/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Query file display name for uri
 *
 * ref: [androidx.documentfile](https://github.com/androidx/androidx/blob/8e30346c2bb3b53a3bd45e9a56f3344d98f2356f/documentfile/documentfile/src/main/java/androidx/documentfile/provider/DocumentsContractApi19.java#L150)
 * @see android.provider.DocumentsContract.Document#COLUMN_DISPLAY_NAME
 */
fun ContentResolver.queryFileName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
    }
