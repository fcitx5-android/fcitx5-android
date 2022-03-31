package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.Typeface
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import splitties.views.imageResource

class NumberKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "Number"

        private val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)

        class NumPadKey(
            displayText: String,
            val sym: UInt,
            percentWidth: Float = 0.1f
        ) : KeyDef(
            Appearance.Text(
                displayText,
                textSize = 16f,
                typeface = Typeface.NORMAL,
                percentWidth
            ),
            Behavior.Press(
                action = KeyAction.SymAction(KeySym(sym), NumLockState)
            )
        )

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                NumPadKey("+", 0xffabu, 0.15f),
                NumPadKey("1", 0xffb1u, 0f),
                NumPadKey("2", 0xffb2u, 0f),
                NumPadKey("3", 0xffb3u, 0f),
                NumPadKey("/", 0xffafu, 0.15f),
            ),
            listOf(
                NumPadKey("-", 0xffadu, 0.15f),
                NumPadKey("4", 0xffb4u, 0f),
                NumPadKey("5", 0xffb5u, 0f),
                NumPadKey("6", 0xffb6u, 0f),
                MiniSpaceKey()
            ),
            listOf(
                NumPadKey("*", 0xffaau, 0.15f),
                NumPadKey("7", 0xffb7u, 0f),
                NumPadKey("8", 0xffb8u, 0f),
                NumPadKey("9", 0xffb9u, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                SymbolKey("#"),
                LayoutSwitchKey("?123", NumSymKeyboard.Name, 0.13333f),
                NumPadKey("0", 0xffb0u, 0.23334f),
                SymbolKey("=", 0.13333f),
                NumPadKey(".", 0xffaeu),
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