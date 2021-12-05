package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.widget.Button
import android.widget.ImageButton
import me.rocka.fcitx5test.R

class NumberKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        val Layout: List<List<BaseKey>> = listOf(
            listOf(
                TextKey("+", 0.15F),
                TextKey("1", 0F),
                TextKey("2", 0F),
                TextKey("3", 0F),
                TextKey("/", 0.15F),
            ),
            listOf(
                TextKey("-", 0.15F),
                TextKey("4", 0F),
                TextKey("5", 0F),
                TextKey("6", 0F),
                MiniSpaceKey()
            ),
            listOf(
                TextKey("*", 0.15F),
                TextKey("7", 0F),
                TextKey("8", 0F),
                TextKey("9", 0F),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey(),
                TextKey("#", 0F),
                TextKey("0", 0F),
                AltTextKey(".", "=", 0F),
                ReturnKey()
            ),
        )
    }

    val backspace: ImageButton by lazy { findViewById(R.id.button_backspace) }
    val layoutSwitch: ImageButton by lazy { findViewById(R.id.button_layout_switch) }
    val space: Button by lazy { findViewById(R.id.button_mini_space) }
    val `return`: Button by lazy { findViewById(R.id.button_return) }

}