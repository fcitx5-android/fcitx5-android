package me.rocka.fcitx5test.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import splitties.views.imageResource

class NumSymKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "NumSym"

        val Layout: List<List<BaseKey>> = listOf(
            listOf(
                TextKey("1"),
                TextKey("2"),
                TextKey("3"),
                TextKey("4"),
                TextKey("5"),
                TextKey("6"),
                TextKey("7"),
                TextKey("8"),
                TextKey("9"),
                TextKey("0")
            ),
            listOf(
                TextKey("@"),
                TextKey("#"),
                TextKey("$"),
                TextKey("%"),
                TextKey("&"),
                TextKey("-"),
                TextKey("+"),
                TextKey("("),
                TextKey(")"),
                TextKey("/"),
            ),
            listOf(LayoutSwitchKey("=\\<", SymbolKeyboard.Name),
                TextKey("*"),
                TextKey("\""),
                TextKey("'"),
                TextKey(":"),
                TextKey(";"),
                TextKey("!"),
                TextKey("?"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                TextKey(","),
                ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name, 0.1f),
                SpaceKey(),
                TextKey("."),
                ReturnKey()
            )
        )
    }

    val `return`: ImageButton by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.imageResource = drawableForReturn(info)
    }
}