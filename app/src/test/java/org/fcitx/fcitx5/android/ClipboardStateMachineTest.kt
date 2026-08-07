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

    @Test
    fun listeningDisabledAlwaysShowsEnableInstruction() {
        ClipboardPanelSection.entries.forEach { section ->
            listOf(true, false).forEach { empty ->
                assertEquals(
                    ClipboardStateMachine.State.EnableListening,
                    ClipboardStateMachine.resolve(false, section, empty)
                )
            }
        }
    }

    @Test
    fun nonEmptySectionShowsEntries() {
        ClipboardPanelSection.entries.forEach { section ->
            assertEquals(
                ClipboardStateMachine.State.Normal,
                ClipboardStateMachine.resolve(true, section, false)
            )
        }
    }

    @Test
    fun emptySectionShowsSectionSpecificInstruction() {
        assertEquals(
            ClipboardStateMachine.State.AddMore,
            ClipboardStateMachine.resolve(true, ClipboardPanelSection.Clipboard, true)
        )
        assertEquals(
            ClipboardStateMachine.State.NoFavorites,
            ClipboardStateMachine.resolve(true, ClipboardPanelSection.Favorites, true)
        )
    }
}
