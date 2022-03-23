package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import splitties.views.imageResource

class NumberKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    companion object {
        const val Name = "Number"
        
        private val NumLockState = KeyStates(KeyState.NumLock)

        val Layout: List<List<BaseKey>> = listOf(
            listOf(
                SymKey("+", 0xffabu, NumLockState, 0.15f),
                SymKey("1", 0xffb1u, NumLockState, 0f),
                SymKey("2", 0xffb2u, NumLockState, 0f),
                SymKey("3", 0xffb3u, NumLockState, 0f),
                SymKey("/", 0xffafu, NumLockState, 0.15f),
            ),
            listOf(
                SymKey("-", 0xffadu, NumLockState, 0.15f),
                SymKey("4", 0xffb4u, NumLockState, 0f),
                SymKey("5", 0xffb5u, NumLockState, 0f),
                SymKey("6", 0xffb6u, NumLockState, 0f),
                MiniSpaceKey()
            ),
            listOf(
                SymKey("*", 0xffaau, NumLockState, 0.15f),
                SymKey("7", 0xffb7u, NumLockState, 0f),
                SymKey("8", 0xffb8u, NumLockState, 0f),
                SymKey("9", 0xffb9u, NumLockState, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                TextKey("#"),
                LayoutSwitchKey("?123", NumSymKeyboard.Name, 0.13333f),
                SymKey("0", 0xffb0u, NumLockState, 0.23334f),
                TextKey("=", 0.13333f),
                SymKey(".", 0xffaeu, NumLockState),
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