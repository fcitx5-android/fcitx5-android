/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatDateTime(timeMillis: Long? = null): String {
    return SimpleDateFormat.getDateTimeInstance().format(timeMillis?.let { Date(it) } ?: Date())
}

private val ISO8601DateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

fun iso8601UTCDateTime(timeMillis: Long? = null): String {
    return ISO8601DateFormat.format(timeMillis?.let { Date(it) } ?: Date())
}
