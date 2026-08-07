/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

data class ClipboardEntryFilter(
    val scope: Scope = Scope.All,
    val category: ClipboardCategory? = null
) {
    enum class Scope {
        All,
        Favorites
    }

    companion object {
        val All = ClipboardEntryFilter()
        val Favorites = ClipboardEntryFilter(Scope.Favorites)
    }
}
