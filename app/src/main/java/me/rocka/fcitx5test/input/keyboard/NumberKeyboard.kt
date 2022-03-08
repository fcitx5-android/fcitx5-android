package me.rocka.fcitx5test.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import splitties.views.imageResource

class NumberKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
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
                LayoutSwitchKey(),
                TextKey("#", 0F),
                KPKey("0", 0F),
                AltTextKey(".", "=", 0F),
                ReturnKey()
            ),
        )
    }

    val backspace: ImageButton by lazy { findViewById(R.id.button_backspace) }
    val layoutSwitch: ImageButton by lazy { findViewById(R.id.button_layout_switch) }
    val space: Button by lazy { findViewById(R.id.button_mini_space) }
    val `return`: ImageButton by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.imageResource = drawableForReturn(info)
    }

}