/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

object ClipboardStateMachine {

    enum class State {
        Normal,
        AddMore,
        NoFavorites,
        NoFilteredEntries,
        EnableListening
    }

    fun resolve(
        listening: Boolean,
        section: ClipboardPanelSection,
        categorySelected: Boolean,
        visibleEntriesEmpty: Boolean
    ): State = when {
        !listening -> State.EnableListening
        !visibleEntriesEmpty -> State.Normal
        categorySelected -> State.NoFilteredEntries
        section == ClipboardPanelSection.Favorites -> State.NoFavorites
        else -> State.AddMore
    }
}
