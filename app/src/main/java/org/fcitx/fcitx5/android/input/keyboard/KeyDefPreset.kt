package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant

val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)

class SymbolKey(
    val symbol: String,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null
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
    popup ?: arrayOf(
        Popup.Preview(symbol),
        Popup.Keyboard(symbol)
    )
)

class AlphabetKey(
    val character: String,
    val punctuation: String,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null
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
    popup ?: arrayOf(
        Popup.AltPreview(character, punctuation),
        Popup.Keyboard(character)
    )
)

class AlphabetDigitKey(
    val character: String,
    altText: String,
    val sym: Int,
    popup: Array<Popup>? = null
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = altText,
        textSize = 23f,
        typeface = Typeface.NORMAL
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.SwipeDown(KeyAction.SymAction(KeySym(sym), NumLockState))
    ),
    popup ?: arrayOf(
        Popup.AltPreview(character, altText),
        Popup.Keyboard(character)
    )
) {
    constructor(
        char: String,
        digit: Int,
        popup: Array<Popup>? = null
    ) : this(
        char,
        digit.toString(),
        0xffb0 + digit,
        popup
    )
}

class CapsKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_capslock_none,
        viewId = R.id.button_caps,
        percentWidth = 0.15f,
        variant = Variant.Alternative
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
    variant: Variant = Variant.Alternative
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
    variant: Variant = Variant.Alternative
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_backspace_24,
        percentWidth = percentWidth,
        variant = variant,
        viewId = R.id.button_backspace
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(KeySym(0xff08))),
        Behavior.Repeat(action = KeyAction.SymAction(KeySym(0xff08)))
    )
)

class QuickPhraseKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_format_quote_24,
        variant = Variant.Alternative,
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
        variant = Variant.AltForeground,
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
        border = Border.Special,
        viewId = R.id.button_space
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(KeySym(0x0020))),
        Behavior.LongPress(action = KeyAction.LangSwitchAction)
    )
)

class ReturnKey(percentWidth: Float = 0.15f) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_keyboard_return_24,
        percentWidth = percentWidth,
        variant = Variant.Accent,
        border = Border.Special,
        viewId = R.id.button_return
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(KeySym(0xff0d)))
    )
)

class ImageLayoutSwitchKey(
    @DrawableRes
    icon: Int,
    to: String,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
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
        variant = Variant.Alternative,
        viewId = R.id.button_mini_space
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(KeySym(0x0020)))
    )
)

class NumPadKey(
    displayText: String,
    val sym: Int,
    textSize: Float = 16f,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = textSize,
        typeface = Typeface.NORMAL,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(KeySym(sym), NumLockState))
    )
)
