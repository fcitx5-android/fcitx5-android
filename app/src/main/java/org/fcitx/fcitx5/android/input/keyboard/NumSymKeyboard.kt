package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import splitties.views.imageResource

class NumSymKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "NumSym"

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                SymbolKey("1"),
                SymbolKey("2"),
                SymbolKey("3"),
                SymbolKey("4"),
                SymbolKey("5"),
                SymbolKey("6"),
                SymbolKey("7"),
                SymbolKey("8"),
                SymbolKey("9"),
                SymbolKey("0")
            ),
            listOf(
                SymbolKey("@"),
                SymbolKey("#"),
                SymbolKey("$"),
                SymbolKey("%"),
                SymbolKey("&"),
                SymbolKey("-"),
                SymbolKey("+"),
                SymbolKey("("),
                SymbolKey(")"),
                SymbolKey("/"),
            ),
            listOf(
                LayoutSwitchKey("=\\<", SymbolKeyboard.Name),
                SymbolKey("*"),
                SymbolKey("\""),
                SymbolKey("'"),
                SymbolKey(":"),
                SymbolKey(";"),
                SymbolKey("!"),
                SymbolKey("?"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                SymbolKey(","),
                ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name),
                SpaceKey(),
                SymbolKey("."),
                ReturnKey()
            )
        )
    }

    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.img.imageResource = drawableForReturn(info)
    }
}