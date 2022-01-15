package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.keyboard.InputView
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
                AltTextKey("S", "`"),
                AltTextKey("D", "$"),
                AltTextKey("F", "_"),
                AltTextKey("G", "&"),
                AltTextKey("H", "/"),
                AltTextKey("J", ";"),
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
        private set(value) {
            lastCapsState = field
            field = value
            updateCapsButtonIcon()
        }

    // capsState before last update
    var lastCapsState: CapsState? = null
        private set

    override fun onAction(view: View, action: KeyAction<*>) {
        when (action) {
            is KeyAction.FcitxKeyAction -> transformKeyAction(action)
            is KeyAction.CapsAction -> {
                if (lastCapsState != CapsState.Lock && action.lock)
                    capsState = CapsState.Lock
                else
                    switchCapsState()
            }
            else -> {
            }
        }
        super.onAction(view, action)
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

    // FIXME: need some new API to know exactly whether next enter would be captured by fcitx
    override fun onPreeditChange(info: EditorInfo?, content: InputView.PreeditContent) {
        val hasPreedit = content.preedit.preedit.isNotEmpty()
        // `auxUp` is not empty when switching input methods, ignore it to reduce flicker
        //        || content.aux.auxUp.isNotEmpty()
        `return`.imageResource = if (hasPreedit) {
            R.drawable.ic_baseline_keyboard_return_24
        } else {
            drawableForReturn(info)
        }
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
            CapsState.Once -> CapsState.None
            CapsState.Lock -> CapsState.None
        }
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