/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer

private const val FALLBACK_DESC = "<no description>"

val IClipboardEntryTransformer.desc: String
    get() = runCatching { description }.getOrElse { FALLBACK_DESC }

fun IClipboardEntryTransformer.descEquals(other: IClipboardEntryTransformer): Boolean {
    return try {
        description!! == other.description!!
    } catch (e: Exception) {
        false
    }
}
