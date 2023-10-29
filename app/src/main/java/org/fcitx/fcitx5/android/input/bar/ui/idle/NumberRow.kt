/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef

@SuppressLint("ViewConstructor")
class NumberRow(ctx: Context, theme: Theme) : BaseKeyboard(ctx, theme, Layout) {
    companion object {
        val Layout = listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { digit ->
                KeyDef(
                    KeyDef.Appearance.Text(
                        displayText = digit,
                        textSize = 21f,
                        border = KeyDef.Appearance.Border.Off,
                        margin = false
                    ),
                    setOf(
                        KeyDef.Behavior.Press(KeyAction.SymAction(KeySym(digit.codePointAt(0))))
                    ),
                    arrayOf(KeyDef.Popup.Preview(digit))
                )
            }
        )
    }
}
