package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates

class SymbolKey(
    val symbol: String,
    percentWidth: Float = 0.1f,
    variant: Appearance.Variant = Appearance.Variant.Normal
) : KeyDef(
    Appearance.Text(
        displayText = symbol,
        textSize = 23f,
        typeface = Typeface.NORMAL,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(action = KeyAction.FcitxKeyAction(symbol))
    ),
    arrayOf(
        Popup.Preview(symbol)
    )
)

class AlphabetKey(
    val character: String,
    val punctuation: String,
    variant: Appearance.Variant = Appearance.Variant.Normal
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = punctuation,
        textSize = 23f,
        typeface = Typeface.NORMAL,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.SwipeDown(KeyAction.FcitxKeyAction(punctuation))
    ),
    arrayOf(
        Popup.AltPreview(character, punctuation),
        // TODO: proper symbol map
        Popup.Keyboard(
            arrayOf(
                Popup.Keyboard.Key(character.uppercase(), KeyAction.FcitxKeyAction(character.uppercase())),
                Popup.Keyboard.Key(character.lowercase(), KeyAction.FcitxKeyAction(character.lowercase())),
                Popup.Keyboard.Key(punctuation, KeyAction.FcitxKeyAction(punctuation)),
            )
        )
    )
)

class AlphabetDigitKey(
    val character: String,
    altText: String,
    val sym: UInt,
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = altText,
        textSize = 23f,
        typeface = Typeface.NORMAL
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.SwipeDown(KeyAction.SymAction(sym, NumLockState))
    ),
    arrayOf(
        Popup.AltPreview(character, altText),
        // TODO: proper symbol map
        Popup.Keyboard(
            arrayOf(
                Popup.Keyboard.Key(character.uppercase(), KeyAction.FcitxKeyAction(character.uppercase())),
                Popup.Keyboard.Key(character.lowercase(), KeyAction.FcitxKeyAction(character.lowercase())),
                Popup.Keyboard.Key(altText, KeyAction.FcitxKeyAction(altText)),
            )
        )
    )
) {
    constructor(char: String, digit: Int) : this(char, digit.toString(), (0xffb0 + digit).toUInt())

    companion object {
        private val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)
    }
}

class CapsKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_capslock_none,
        viewId = R.id.button_caps,
        percentWidth = 0.15f,
        variant = Appearance.Variant.Alternative
    ),
    setOf(
        Behavior.Press(action = KeyAction.CapsAction(false)),
        Behavior.LongPress(action = KeyAction.CapsAction(true)),
        Behavior.DoubleTap(action = KeyAction.CapsAction(true))
    )
)

class LayoutSwitchKey(
    displayText: String,
    val to: String = "",
    percentWidth: Float = 0.15f,
    variant: Appearance.Variant = Appearance.Variant.Alternative
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = 16f,
        typeface = Typeface.BOLD,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(action = KeyAction.LayoutSwitchAction(to))
    )
)

class BackspaceKey(
    percentWidth: Float = 0.15f,
    variant: Appearance.Variant = Appearance.Variant.Alternative
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_backspace_24,
        percentWidth = percentWidth,
        variant = variant,
        viewId = R.id.button_backspace
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(0xff08u)),
        Behavior.Repeat(action = KeyAction.SymAction(0xff08u))
    )
)

class QuickPhraseKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_format_quote_24,
        variant = Appearance.Variant.Alternative,
        viewId = R.id.button_quickphrase
    ),
    setOf(
        Behavior.Press(KeyAction.QuickPhraseAction),
        Behavior.LongPress(KeyAction.UnicodeAction)
    )
)

class LanguageKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_language_24,
        variant = Appearance.Variant.AltForeground,
        viewId = R.id.button_lang
    ),
    setOf(
        Behavior.Press(KeyAction.LangSwitchAction),
        Behavior.LongPress(KeyAction.InputMethodSwitchAction)
    )
)

class SpaceKey : KeyDef(
    Appearance.Text(
        displayText = " ",
        textSize = 13f,
        typeface = Typeface.NORMAL,
        percentWidth = 0f,
        forceBordered = true,
        viewId = R.id.button_space
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(0x0020u))
    )
)

class ReturnKey(percentWidth: Float = 0.15f) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_keyboard_return_24,
        percentWidth = percentWidth,
        variant = Appearance.Variant.Accent,
        forceBordered = true,
        viewId = R.id.button_return
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(0xff0du))
    )
)

class ImageLayoutSwitchKey(
    @DrawableRes
    icon: Int,
    to: String,
    percentWidth: Float = 0.1f,
    variant: Appearance.Variant = Appearance.Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId
    ),
    setOf(
        Behavior.Press(action = KeyAction.LayoutSwitchAction(to))
    )
)

class MiniSpaceKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_space_bar_24,
        percentWidth = 0.15f,
        variant = Appearance.Variant.Alternative,
        viewId = R.id.button_mini_space
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(0x0020u))
    )
)

class NumPadKey(
    displayText: String,
    val sym: UInt,
    textSize: Float = 16f,
    percentWidth: Float = 0.1f,
    variant: Appearance.Variant = Appearance.Variant.Normal
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = textSize,
        typeface = Typeface.NORMAL,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(sym, NumLockState))
    )
) {
    companion object {
        private val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)
    }
}