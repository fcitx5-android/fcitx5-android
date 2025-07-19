/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import org.fcitx.fcitx5.android.input.popup.PopupAction
import splitties.views.imageResource

@SuppressLint("ViewConstructor")
class NumberKeyboard(
    context: Context,
    theme: Theme,
) : BaseKeyboard(context, theme, Layout) {

    companion object {
        const val Name = "Number"

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                NumPadSymbolKey("+", 0xffab, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadNumberKey("1", 0xffb1, 0f),
                NumPadNumberKey("2", 0xffb2, 0f),
                NumPadNumberKey("3", 0xffb3, 0f),
                NumPadSymbolKey("/", 0xffaf, 0.15f, KeyDef.Appearance.Variant.Alternative),
            ),
            listOf(
                NumPadSymbolKey("-", 0xffad, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadNumberKey("4", 0xffb4, 0f),
                NumPadNumberKey("5", 0xffb5, 0f),
                NumPadNumberKey("6", 0xffb6, 0f),
                MiniSpaceKey()
            ),
            listOf(
                NumPadSymbolKey("*", 0xffaa, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadNumberKey("7", 0xffb7, 0f),
                NumPadNumberKey("8", 0xffb8, 0f),
                NumPadNumberKey("9", 0xffb9, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                NumPadSymbolKey(",", 0xffac, 0.1f, KeyDef.Appearance.Variant.Alternative),
                LayoutSwitchKey(
                    "!?#",
                    PickerWindow.Key.Symbol.name,
                    0.13333f,
                    KeyDef.Appearance.Variant.AltForeground
                ),
                NumPadNumberKey("0", 0xffb0, 0.23334f),
                NumPadSymbolKey("=", 0xffbd, 0.13333f, KeyDef.Appearance.Variant.AltForeground),
                NumPadSymbolKey(".", 0xffae, 0.1f, KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            )
        )
    }

    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val space: TextKeyView by lazy { findViewById(R.id.button_mini_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onReturnDrawableUpdate(returnDrawable: Int) {
        `return`.img.imageResource = returnDrawable
    }

    @SuppressLint("MissingSuperCall")
    override fun onPopupAction(action: PopupAction) {
        // leave empty on purpose to disable popup in NumberKeyboard
    }

}