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
) : BaseKeyboard(context, theme, Layout, SplitRows, SplitLayout) {

    companion object {
        const val Name = "Number"

        val SplitRows = listOf(
            SplitRowSpec(4),
            SplitRowSpec(4),
            SplitRowSpec(4),
            TextKeyboard.SplitBottomRow.copy(
                rightCols = 4,
                rightIndent = 0f,
                expandOuterRight = false
            )
        )

        private fun splitSymbol(symbol: String) = SymbolKey(
            symbol,
            variant = KeyDef.Appearance.Variant.Alternative,
            popup = emptyArray()
        )

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                NumPadKey("+", 0xffab, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("1", 0xffb1, 30f, 0f),
                NumPadKey("2", 0xffb2, 30f, 0f),
                NumPadKey("3", 0xffb3, 30f, 0f),
                NumPadKey("/", 0xffaf, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
            ),
            listOf(
                NumPadKey("-", 0xffad, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("4", 0xffb4, 30f, 0f),
                NumPadKey("5", 0xffb5, 30f, 0f),
                NumPadKey("6", 0xffb6, 30f, 0f),
                MiniSpaceKey()
            ),
            listOf(
                NumPadKey("*", 0xffaa, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("7", 0xffb7, 30f, 0f),
                NumPadKey("8", 0xffb8, 30f, 0f),
                NumPadKey("9", 0xffb9, 30f, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                NumPadKey(",", 0xffac, 23f, 0.1f, KeyDef.Appearance.Variant.Alternative),
                LayoutSwitchKey("!?#", PickerWindow.Key.Symbol.name, 0.13333f, KeyDef.Appearance.Variant.AltForeground),
                NumPadKey("0", 0xffb0, 30f, 0.23334f),
                NumPadKey("=", 0xffbd, 23f, 0.13333f, KeyDef.Appearance.Variant.AltForeground),
                NumPadKey(".", 0xffae, 23f, 0.1f, KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            )
        )

        val SplitLayout: List<List<KeyDef>> = listOf(
            listOf(
                splitSymbol("-"),
                splitSymbol("+"),
                splitSymbol("*"),
                splitSymbol("/"),
                NumPadKey("1", 0xffb1, 30f, 0f),
                NumPadKey("2", 0xffb2, 30f, 0f),
                NumPadKey("3", 0xffb3, 30f, 0f),
                splitSymbol("%")
            ),
            listOf(
                splitSymbol("("),
                splitSymbol(")"),
                splitSymbol("{"),
                splitSymbol("}"),
                NumPadKey("4", 0xffb4, 30f, 0f),
                NumPadKey("5", 0xffb5, 30f, 0f),
                NumPadKey("6", 0xffb6, 30f, 0f),
                splitSymbol("=")
            ),
            listOf(
                splitSymbol("√"),
                splitSymbol("π"),
                splitSymbol("×"),
                splitSymbol("÷"),
                NumPadKey("7", 0xffb7, 30f, 0f),
                NumPadKey("8", 0xffb8, 30f, 0f),
                NumPadKey("9", 0xffb9, 30f, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                NumPadKey(",", 0xffac, 23f, variant = KeyDef.Appearance.Variant.Alternative),
                LayoutSwitchKey("!?#", PickerWindow.Key.Symbol.name, variant = KeyDef.Appearance.Variant.AltForeground),
                SpaceKey(),
                NumPadKey("0", 0xffb0, 30f),
                NumPadKey(".", 0xffae, 23f, variant = KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            )
        )
    }

    val backspace: ImageKeyView get() = findViewById(R.id.button_backspace)
    val space: TextKeyView get() = findViewById<TextKeyView>(R.id.button_space)
        ?: findViewById(R.id.button_mini_space)
    val `return`: ImageKeyView get() = findViewById(R.id.button_return)

    private var returnDrawableId = R.drawable.ic_baseline_keyboard_return_24

    override fun onReturnDrawableUpdate(returnDrawable: Int) {
        returnDrawableId = returnDrawable
        `return`.img.imageResource = returnDrawable
    }

    override fun onSplitLayoutChanged() {
        `return`.img.imageResource = returnDrawableId
    }

    @SuppressLint("MissingSuperCall")
    override fun onPopupAction(action: PopupAction) {
        // leave empty on purpose to disable popup in NumberKeyboard
    }

}
