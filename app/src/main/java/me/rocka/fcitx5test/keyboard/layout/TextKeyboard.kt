package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.InputMethodEntry
import splitties.views.imageResource

class TextKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    enum class CapsState { None, Once, Lock }

    companion object {
        val Layout: List<List<BaseKey>> = listOf(
            listOf(
                AltTextKey("Q", "1"),
                AltTextKey("W", "2"),
                AltTextKey("E", "3"),
                AltTextKey("R", "4"),
                AltTextKey("T", "5"),
                AltTextKey("Y", "6"),
                AltTextKey("U", "7"),
                AltTextKey("I", "8"),
                AltTextKey("O", "9"),
                AltTextKey("P", "0")
            ),
            listOf(
                AltTextKey("A", "@"),
                AltTextKey("S", "*"),
                AltTextKey("D", "+"),
                AltTextKey("F", "-"),
                AltTextKey("G", "="),
                AltTextKey("H", "/"),
                AltTextKey("J", "#"),
                AltTextKey("K", "("),
                AltTextKey("L", ")")
            ),
            listOf(
                CapsKey(),
                AltTextKey("Z", "'"),
                AltTextKey("X", ":"),
                AltTextKey("C", "\""),
                AltTextKey("V", "?"),
                AltTextKey("B", "!"),
                AltTextKey("N", "~"),
                AltTextKey("M", "\\"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey(),
                QuickPhraseKey(),
                LangSwitchKey(),
                SpaceKey(),
                AltTextKey(",", "."),
                ReturnKey()
            ),
        )
    }

    val caps: ImageButton by lazy { findViewById(R.id.button_caps) }
    val backspace: ImageButton by lazy { findViewById(R.id.button_backspace) }
    val layoutSwitch: ImageButton by lazy { findViewById(R.id.button_layout_switch) }
    val quickphrase: ImageButton by lazy { findViewById(R.id.button_quickphrase) }
    val lang: ImageButton by lazy { findViewById(R.id.button_lang) }
    val space: Button by lazy { findViewById(R.id.button_space) }
    val `return`: ImageButton by lazy { findViewById(R.id.button_return) }

    var capsState: CapsState = CapsState.None

    override fun onAction(view: View, action: KeyAction<*>, long: Boolean) {
        when (action) {
            is KeyAction.FcitxKeyAction -> transformKeyAction(action)
            is KeyAction.CapsAction -> switchCapsState()
            else -> {}
        }
        super.onAction(view, action, long)
    }

    private fun transformKeyAction(action: KeyAction.FcitxKeyAction) {
        if (action.act.length > 1) {
            return
        }
        when (capsState) {
            CapsState.None -> action.lower()
            CapsState.Once -> {
                capsState = CapsState.None
                updateCapsButtonIcon()
                action.upper()
            }
            CapsState.Lock -> action.upper()
        }
    }

    override fun onAttach(info: EditorInfo?) {
        `return`.imageResource = drawableForReturn(info)
    }

    override fun onInputMethodChange(ime: InputMethodEntry) {
        val s = StringBuilder(ime.displayName)
        ime.subMode.run {
            when {
                label.isNotEmpty() -> label
                name.isNotEmpty() -> name
                else -> null
            }?.let {
                s.append(" ($it)")
            }
        }
        space.text = s
    }

    private fun switchCapsState() {
        capsState = when (capsState) {
            CapsState.None -> CapsState.Once
            CapsState.Once -> CapsState.Lock
            CapsState.Lock -> CapsState.None
        }
        updateCapsButtonIcon()
    }

    private fun updateCapsButtonIcon() {
        caps.setImageResource(
            when (capsState) {
                CapsState.None -> R.drawable.ic_baseline_keyboard_capslock0_24
                CapsState.Once -> R.drawable.ic_baseline_keyboard_capslock1_24
                CapsState.Lock -> R.drawable.ic_baseline_keyboard_capslock2_24
            }
        )
    }

}