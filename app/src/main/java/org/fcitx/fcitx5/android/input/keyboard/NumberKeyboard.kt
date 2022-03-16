package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import org.fcitx.fcitx5.android.R
import splitties.views.imageResource

class NumberKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "Number"

        val Layout: List<List<BaseKey>> = listOf(
            listOf(
                KPKey("Add", "+", 0.15F),
                KPKey("1", 0F),
                KPKey("2", 0F),
                KPKey("3", 0F),
                KPKey("Divide", "/", 0.15F),
            ),
            listOf(
                KPKey("Subtract", "-", 0.15F),
                KPKey("4", 0F),
                KPKey("5", 0F),
                KPKey("6", 0F),
                MiniSpaceKey()
            ),
            listOf(
                KPKey("Multiply", "*", 0.15F),
                KPKey("7", 0F),
                KPKey("8", 0F),
                KPKey("9", 0F),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                TextKey("#"),
                LayoutSwitchKey("?123", NumSymKeyboard.Name, 0.13333F),
                KPKey("0", 0.23334F),
                TextKey("=", 0.13333F),
                KPKey("Decimal", "."),
                ReturnKey()
            ),
        )
    }

    val backspace: ImageButton by lazy { findViewById(R.id.button_backspace) }
    val space: Button by lazy { findViewById(R.id.button_mini_space) }
    val `return`: ImageButton by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.imageResource = drawableForReturn(info)
    }

}