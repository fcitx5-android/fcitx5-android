package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent

class NumberKeyboard(
    context: Context,
    fcitx: Fcitx,
    passAction: (View, KeyAction<*>, Boolean) -> Unit
) : BaseKeyboard(context, fcitx, Preset.T9, passAction) {

    val backspace: ImageButton by lazy { findViewById(R.id.button_backspace) }
    val layoutSwitch: ImageButton by lazy { findViewById(R.id.button_layout_switch) }
    val space: Button by lazy { findViewById(R.id.button_mini_space) }
    val `return`: Button by lazy { findViewById(R.id.button_return) }


    override fun handleFcitxEvent(event: FcitxEvent<*>) {
        // nothing to do
    }

    override fun onAction(v: View, it: KeyAction<*>, long: Boolean) {
        super.onAction(v, it, long)
        when (it) {
            is KeyAction.FcitxKeyAction -> onKeyPress(it.act)
            is KeyAction.BackspaceAction -> backspace()
            is KeyAction.ReturnAction -> enter()
            else -> {}
        }
    }

}