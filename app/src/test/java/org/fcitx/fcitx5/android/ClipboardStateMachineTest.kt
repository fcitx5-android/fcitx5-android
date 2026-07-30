/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.input.clipboard.ClipboardPanelSection
import org.fcitx.fcitx5.android.input.clipboard.ClipboardStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardStateMachineTest {
    private val clipboardSections = listOf(
        ClipboardPanelSection.Clipboard,
        ClipboardPanelSection.Favorites
    )

    @Test
    fun listeningDisabledAlwaysShowsEnableInstruction() {
        clipboardSections.forEach { section ->
            listOf(true, false).forEach { empty ->
                assertEquals(
                    ClipboardStateMachine.State.EnableListening,
                    ClipboardStateMachine.resolve(false, section, false, empty)
                )
            }
        }
    }

    @Test
    fun nonEmptySectionShowsEntries() {
        clipboardSections.forEach { section ->
            assertEquals(
                ClipboardStateMachine.State.Normal,
                ClipboardStateMachine.resolve(true, section, false, false)
            )
        }
    }

    @Test
    fun emptySectionShowsSectionSpecificInstruction() {
        assertEquals(
            ClipboardStateMachine.State.AddMore,
            ClipboardStateMachine.resolve(true, ClipboardPanelSection.Clipboard, false, true)
        )
        assertEquals(
            ClipboardStateMachine.State.NoFavorites,
            ClipboardStateMachine.resolve(true, ClipboardPanelSection.Favorites, false, true)
        )
    }

    @Test
    fun emptyCategoryShowsFilteredInstructionInBothSections() {
        clipboardSections.forEach { section ->
            assertEquals(
                ClipboardStateMachine.State.NoFilteredEntries,
                ClipboardStateMachine.resolve(true, section, true, true)
            )
        }
    }
}
