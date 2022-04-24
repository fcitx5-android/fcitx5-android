package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
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
                NumPadKey("+", 0xffabu, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("1", 0xffb1u, 30f, 0f),
                NumPadKey("2", 0xffb2u, 30f, 0f),
                NumPadKey("3", 0xffb3u, 30f, 0f),
                NumPadKey("/", 0xffafu, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
            ),
            listOf(
                NumPadKey("-", 0xffadu, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("4", 0xffb4u, 30f, 0f),
                NumPadKey("5", 0xffb5u, 30f, 0f),
                NumPadKey("6", 0xffb6u, 30f, 0f),
                MiniSpaceKey()
            ),
            listOf(
                NumPadKey("*", 0xffaau, 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("7", 0xffb7u, 30f, 0f),
                NumPadKey("8", 0xffb8u, 30f, 0f),
                NumPadKey("9", 0xffb9u, 30f, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                SymbolKey(",", variant = KeyDef.Appearance.Variant.Alternative),
                LayoutSwitchKey(
                    "!?#",
                    NumSymKeyboard.Name,
                    percentWidth = 0.13333f,
                    variant = KeyDef.Appearance.Variant.Normal
                ),
                NumPadKey("0", 0xffb0u, 30f, 0.23334f),
                SymbolKey("=", 0.13333f),
                NumPadKey(".", 0xffaeu, 23f, 0.1f, KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            ),
        )
    }

    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val space: TextKeyView by lazy { findViewById(R.id.button_mini_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.img.imageResource = drawableForReturn(info)
    }

}