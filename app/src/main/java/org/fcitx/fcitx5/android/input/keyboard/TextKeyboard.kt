package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.views.imageDrawable
import splitties.views.imageResource

class TextKeyboard(
    context: Context
) : BaseKeyboard(context, Layout) {

    enum class CapsState { None, Once, Lock }

    companion object {
        const val Name = "Text"

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                AlphabetDigitKey("Q", 1),
                AlphabetDigitKey("W", 2),
                AlphabetDigitKey("E", 3),
                AlphabetDigitKey("R", 4),
                AlphabetDigitKey("T", 5),
                AlphabetDigitKey("Y", 6),
                AlphabetDigitKey("U", 7),
                AlphabetDigitKey("I", 8),
                AlphabetDigitKey("O", 9),
                AlphabetDigitKey("P", 0)
            ),
            listOf(
                AlphabetKey("A", "@"),
                AlphabetKey("S", "`"),
                AlphabetKey("D", "$"),
                AlphabetKey("F", "_"),
                AlphabetKey("G", "&"),
                AlphabetKey("H", "/"),
                AlphabetKey("J", ";"),
                AlphabetKey("K", "("),
                AlphabetKey("L", ")")
            ),
            listOf(
                CapsKey(),
                AlphabetKey("Z", "'"),
                AlphabetKey("X", ":"),
                AlphabetKey("C", "\""),
                AlphabetKey("V", "?"),
                AlphabetKey("B", "!"),
                AlphabetKey("N", "~"),
                AlphabetKey("M", "\\"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("?123", ""),
                QuickPhraseKey(),
                LanguageKey(),
                SpaceKey(),
                AlphabetKey(",", "."),
                ReturnKey()
            ),
        )
    }

    val caps: ImageKeyView by lazy { findViewById(R.id.button_caps) }
    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val quickphrase: ImageKeyView by lazy { findViewById(R.id.button_quickphrase) }
    val lang: ImageKeyView by lazy { findViewById(R.id.button_lang) }
    val space: TextKeyView by lazy { findViewById(R.id.button_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    var capsState: CapsState = CapsState.None
        private set(value) {
            lastCapsState = field
            field = value
            updateCapsButtonIcon()
        }

    // capsState before last update
    var lastCapsState: CapsState? = null
        private set

    override fun onAction(action: KeyAction) {
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
        super.onAction(action)
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
        updateCapsButtonIcon()
        `return`.img.imageResource = drawableForReturn(info)
    }

    override fun onEditorInfoChange(info: EditorInfo?) {
        `return`.img.imageResource = drawableForReturn(info)
    }

    override fun onPreeditChange(info: EditorInfo?, content: PreeditContent) {
        updateReturnButton(`return`, info, content)
    }

    override fun onInputMethodChange(ime: InputMethodEntry) {
        val s = StringBuilder(ime.displayName)
        ime.subMode
            .run { label.ifEmpty { name.ifEmpty { null } } }
            ?.let { s.append(" ($it)") }
        space.mainText.text = s
    }

    private fun switchCapsState() {
        capsState = when (capsState) {
            CapsState.None -> CapsState.Once
            CapsState.Once -> CapsState.None
            CapsState.Lock -> CapsState.None
        }
    }

    private fun updateCapsButtonIcon() {
        caps.img.apply {
            imageDrawable = drawable(
                when (capsState) {
                    CapsState.None -> R.drawable.ic_baseline_expand_less_24
                    CapsState.Once, CapsState.Lock -> R.drawable.ic_baseline_keyboard_capslock_24
                }
            )
            colorFilter = PorterDuffColorFilter(
                styledColor(
                    when (capsState) {
                        CapsState.None, CapsState.Once -> android.R.attr.colorControlNormal
                        CapsState.Lock -> android.R.attr.colorAccent
                    }
                ), PorterDuff.Mode.SRC_IN
            )
        }
    }

}