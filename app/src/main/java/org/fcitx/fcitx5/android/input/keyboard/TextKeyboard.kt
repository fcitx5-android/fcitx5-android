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
                AlphaBetKey("Q", "1"),
                AlphaBetKey("W", "2"),
                AlphaBetKey("E", "3"),
                AlphaBetKey("R", "4"),
                AlphaBetKey("T", "5"),
                AlphaBetKey("Y", "6"),
                AlphaBetKey("U", "7"),
                AlphaBetKey("I", "8"),
                AlphaBetKey("O", "9"),
                AlphaBetKey("P", "0")
            ),
            listOf(
                AlphaBetKey("A", "@"),
                AlphaBetKey("S", "`"),
                AlphaBetKey("D", "$"),
                AlphaBetKey("F", "_"),
                AlphaBetKey("G", "&"),
                AlphaBetKey("H", "/"),
                AlphaBetKey("J", ";"),
                AlphaBetKey("K", "("),
                AlphaBetKey("L", ")")
            ),
            listOf(
                CapsKey(),
                AlphaBetKey("Z", "'"),
                AlphaBetKey("X", ":"),
                AlphaBetKey("C", "\""),
                AlphaBetKey("V", "?"),
                AlphaBetKey("B", "!"),
                AlphaBetKey("N", "~"),
                AlphaBetKey("M", "\\"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("?123", ""),
                QuickPhraseKey(),
                LanguageKey(),
                SpaceKey(),
                AlphaBetKey(",", "."),
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
        ime.subMode.run {
            when {
                label.isNotEmpty() -> label
                name.isNotEmpty() -> name
                else -> null
            }?.let {
                s.append(" ($it)")
            }
        }
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