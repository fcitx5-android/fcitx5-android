package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.data.theme.ThemeManager

class SymbolKey(
    val symbol: String,
    percentWidth: Float = 0.1f
) : KeyDef(
    Appearance.Text(
        displayText = symbol,
        textSize = 20f,
        typeface = Typeface.NORMAL,
        percentWidth
    ),
    setOf(
        Behavior.Press(action = KeyAction.FcitxKeyAction(symbol))
    )
)

class AlphabetKey(
    val character: String,
    val punctuation: String
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = punctuation,
        textSize = 20f,
        typeface = Typeface.NORMAL
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.LongPress(KeyAction.FcitxKeyAction(punctuation)),
        Behavior.SwipeDown(KeyAction.FcitxKeyAction(punctuation))
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
        textSize = 20f,
        typeface = Typeface.NORMAL
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.LongPress(KeyAction.SymAction(sym, NumLockState)),
        Behavior.SwipeDown(KeyAction.SymAction(sym, NumLockState))
    )
) {
    constructor(char: String, digit: Int) : this(char, digit.toString(), (0xffb0 + digit).toUInt())

    companion object {
        private val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)
    }
}

class CapsKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_keyboard_capslock_24,
        viewId = R.id.button_caps,
        percentWidth = 0.15f
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
    percentWidth: Float = 0.15f
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = 16f,
        typeface = Typeface.BOLD,
        percentWidth,
        textColor = ThemeManager.currentTheme.funKeyColor,
    ),
    setOf(
        Behavior.Press(action = KeyAction.LayoutSwitchAction(to))
    )
)

class BackspaceKey(percentWidth: Float = 0.15f) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_backspace_24,
        percentWidth = percentWidth,
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
        background = ThemeManager.currentTheme.keyBackgroundColorBordered,
        viewId = R.id.button_space
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(0x0020u))
    )
)

class ReturnKey(percentWidth: Float = 0.15f) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_keyboard_return_24,
        tint = ThemeManager.currentTheme.keyTextColorInverse,
        percentWidth,
        background = ThemeManager.currentTheme.keyAccentBackgroundColor,
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
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
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
        viewId = R.id.button_mini_space
    ),
    setOf(
        Behavior.Press(action = KeyAction.SymAction(0x0020u))
    )
)

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
    setOf(
        Behavior.Press(action = KeyAction.SymAction(sym, NumLockState))
    )
) {
    companion object {
        private val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)
    }
}