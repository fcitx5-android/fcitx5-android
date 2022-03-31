package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import splitties.views.imageResource

class SymbolKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "Symbol"

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                SymbolKey("~"),
                SymbolKey("`"),
                SymbolKey("|"),
                SymbolKey("·"),
                SymbolKey("√"),
                SymbolKey("π"),
                SymbolKey("÷"),
                SymbolKey("×"),
                SymbolKey("¶"),
                SymbolKey("∆")
            ),
            listOf(
                SymbolKey("¥"),
                SymbolKey("£"),
                SymbolKey("€"),
                SymbolKey("¢"),
                SymbolKey("^"),
                SymbolKey("°"),
                SymbolKey("="),
                SymbolKey("{"),
                SymbolKey("}"),
                SymbolKey("\\"),
            ),
            listOf(
                LayoutSwitchKey("?123", NumSymKeyboard.Name),
                SymbolKey("_"),
                SymbolKey("©"),
                SymbolKey("®"),
                SymbolKey("™"),
                SymbolKey("✓"),
                SymbolKey("["),
                SymbolKey("]"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                SymbolKey("<"),
                ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name, 0.1f),
                SpaceKeyDef(),
                SymbolKey(">"),
                ReturnKey()
            )
        )
    }

    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onAttach(info: EditorInfo?) {
        `return`.img.imageResource = drawableForReturn(info)
    }
}