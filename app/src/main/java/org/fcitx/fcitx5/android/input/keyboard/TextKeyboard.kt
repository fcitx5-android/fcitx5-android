package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.inputmethod.EditorInfo
import androidx.core.view.allViews
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.resources.drawable
import splitties.views.imageDrawable
import splitties.views.imageResource

@SuppressLint("ViewConstructor")
class TextKeyboard(
    context: Context,
    theme: Theme
) : BaseKeyboard(context, theme, Layout) {

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
                AlphabetKey("S", "*"),
                AlphabetKey("D", "+"),
                AlphabetKey("F", "-"),
                AlphabetKey("G", "="),
                AlphabetKey("H", "/"),
                AlphabetKey("J", "#"),
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
                AlphabetKey(".", ",", KeyDef.Appearance.Variant.Alternative),
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

    private val textKeys: List<AltTextKeyView> by lazy {
        allViews.filterIsInstance(AltTextKeyView::class.java).toList()
    }

    private var capsState: CapsState = CapsState.None

    private fun transformAlphabet(c: String): String {
        return when (capsState) {
            CapsState.None -> c.lowercase()
            else -> c.uppercase()
        }
    }

    private fun transformPunctuation(p: String) = punctuation?.transform(p) ?: p

    private fun transformInputString(c: String): String {
        if (c.length != 1) return c
        if (c[0].isLetter()) return transformAlphabet(c)
        return transformPunctuation(c)
    }

    override fun onAction(action: KeyAction, source: KeyActionListener.Source) {
        when (action) {
            is KeyAction.FcitxKeyAction -> if (source == KeyActionListener.Source.Keyboard) {
                transformKeyAction(action)
            }
            is KeyAction.CapsAction -> switchCapsState(action.lock)
            else -> {
            }
        }
        super.onAction(action, source)
    }

    private fun transformKeyAction(action: KeyAction.FcitxKeyAction) {
        if (action.act.length > 1) {
            return
        }
        action.act = transformAlphabet(action.act)
        if (capsState == CapsState.Once) switchCapsState()
    }

    override fun onAttach(info: EditorInfo?) {
        updateCapsButtonIcon()
        updateAlphabetKeys()
        `return`.img.imageResource = drawableForReturn(info)
    }

    override fun onEditorInfoChange(info: EditorInfo?) {
        `return`.img.imageResource = drawableForReturn(info)
    }

    override fun onPreeditChange(info: EditorInfo?, data: FcitxEvent.PreeditEvent.Data) {
        updateReturnButton(`return`, info, data)
    }

    override fun onStatusAreaUpdate(actions: Array<Action>) {
        updatePunctuationKeys()
    }

    override fun onInputMethodChange(ime: InputMethodEntry) {
        space.mainText.text = buildString {
            append(ime.displayName)
            ime.subMode.run { label.ifEmpty { name.ifEmpty { null } } }?.let { append(" ($it)") }
        }
        updatePunctuationKeys()
    }

    override fun onPopupPreview(viewId: Int, content: String, bounds: Rect) {
        super.onPopupPreview(viewId, transformInputString(content), bounds)
    }

    override fun onPopupPreviewUpdate(viewId: Int, content: String) {
        super.onPopupPreviewUpdate(viewId, transformInputString(content))
    }

    override fun onPopupKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect) {
        val label = keyboard.label
        val k = if (label.length == 1 && label[0].isLetter())
            KeyDef.Popup.Keyboard(transformAlphabet(label))
        else keyboard
        super.onPopupKeyboard(viewId, k, bounds)
    }

    private fun switchCapsState(lock: Boolean = false) {
        capsState = if (lock) when (capsState) {
            CapsState.Lock -> CapsState.None
            else -> CapsState.Lock
        } else when (capsState) {
            CapsState.None -> CapsState.Once
            else -> CapsState.None
        }
        updateCapsButtonIcon()
        updateAlphabetKeys()
    }

    private fun updateCapsButtonIcon() {
        caps.img.apply {
            imageDrawable = drawable(
                when (capsState) {
                    CapsState.None -> R.drawable.ic_capslock_none
                    CapsState.Once -> R.drawable.ic_capslock_once
                    CapsState.Lock -> R.drawable.ic_capslock_lock
                }
            )
        }
    }

    private val keepLettersUppercase by AppPrefs.getInstance().keyboard.keepLettersUppercase

    private fun updateAlphabetKeys() {
        textKeys.forEach {
            it.def as KeyDef.Appearance.AltText
            it.mainText.text = it.def.displayText.let { str ->
                if (str.length != 1 || !str[0].isLetter()) return@forEach
                if (keepLettersUppercase) str.uppercase() else transformAlphabet(str)
            }
        }
    }

    private fun updatePunctuationKeys() {
        textKeys.forEach {
            it.def as KeyDef.Appearance.AltText
            it.altText.text = transformPunctuation(it.def.altText)
            it.mainText.text = it.def.displayText.let { str ->
                if (str[0].isLetter()) return@forEach
                transformPunctuation(str)
            }
        }
    }

}