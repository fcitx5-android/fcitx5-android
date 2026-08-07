/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard.db

import androidx.room.TypeConverter
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory

class ClipboardConverters {
    @TypeConverter
    fun categoryToString(category: ClipboardCategory): String = category.name

    @TypeConverter
    fun stringToCategory(value: String): ClipboardCategory =
        ClipboardCategory.entries.firstOrNull { it.name == value } ?: ClipboardCategory.Other
}
