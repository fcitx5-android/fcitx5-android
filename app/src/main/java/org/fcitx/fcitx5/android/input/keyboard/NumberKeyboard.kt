package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.picker.PickerWindow
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
                SymbolKey(",", variant = KeyDef.Appearance.Variant.Alternative),
                LayoutSwitchKey("!?#", PickerWindow.Symbol.name, 0.13333f, KeyDef.Appearance.Variant.AltForeground),
                NumPadKey("0", 0xffb0, 30f, 0.23334f),
                SymbolKey("=", 0.13333f),
                NumPadKey(".", 0xffae, 23f, 0.1f, KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            )
        )
    }

    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val space: TextKeyView by lazy { findViewById(R.id.button_mini_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.img.imageResource = drawableForReturn(info)
    }

}