package me.rocka.fcitx5test.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import splitties.views.imageResource

class SymbolKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "Symbol"

        val Layout: List<List<BaseKey>> = listOf(
            listOf(
                TextKey("~"),
                TextKey("`"),
                TextKey("|"),
                TextKey("·"),
                TextKey("√"),
                TextKey("π"),
                TextKey("÷"),
                TextKey("×"),
                TextKey("¶"),
                TextKey("∆")
            ),
            listOf(
                TextKey("¥"),
                TextKey("£"),
                TextKey("€"),
                TextKey("¢"),
                TextKey("^"),
                TextKey("°"),
                TextKey("="),
                TextKey("{"),
                TextKey("}"),
                TextKey("\\"),
            ),
            listOf(
                LayoutSwitchKey("?123", NumSymKeyboard.Name),
                TextKey("_"),
                TextKey("©"),
                TextKey("®"),
                TextKey("™"),
                TextKey("✓"),
                TextKey("["),
                TextKey("]"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                TextKey("<"),
                ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name, 0.1f),
                SpaceKey(),
                TextKey(">"),
                ReturnKey()
            )
        )
    }

    val `return`: ImageButton by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.imageResource = drawableForReturn(info)
    }
}