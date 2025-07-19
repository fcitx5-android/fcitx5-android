/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.picker.PickerWindow

val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)

class SymbolKey(
    val symbol: String,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null
) : KeyDef(
    Appearance.Symbol(symbol, percentWidth, variant),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(symbol))
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
    Appearance.Alphabet(character, punctuation, variant),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.Swipe(KeyAction.FcitxKeyAction(punctuation))
    ),
    popup ?: arrayOf(
        Popup.AltPreview(character, punctuation),
        Popup.Keyboard(character)
    )
)

class CapsKey : KeyDef(
    Appearance.Caps,
    setOf(
        Behavior.Press(KeyAction.CapsAction(false)),
        Behavior.LongPress(KeyAction.CapsAction(true)),
        Behavior.DoubleTap(KeyAction.CapsAction(true))
    )
)

class LayoutSwitchKey(
    displayText: String,
    val to: String = "",
    percentWidth: Float = 0.15f,
    variant: Variant = Variant.Alternative
) : KeyDef(
    Appearance.TextLayoutSwitch(
        displayText,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.LayoutSwitchAction(to))
    )
)

class BackspaceKey(
    percentWidth: Float = 0.15f,
    variant: Variant = Variant.Alternative
) : KeyDef(
    Appearance.Backspace(percentWidth, variant),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace))),
        Behavior.Repeat(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace)))
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

class CommaKey(
    percentWidth: Float,
    variant: Variant,
) : KeyDef(
    Appearance.Comma(percentWidth, variant),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(","))
    ),
    arrayOf(
        Popup.Preview(","),
        Popup.Menu(
            arrayOf(
                Popup.Menu.Item(
                    "Emoji",
                    R.drawable.ic_baseline_tag_faces_24,
                    KeyAction.PickerSwitchAction()
                ),
                Popup.Menu.Item(
                    "QuickPhrase",
                    R.drawable.ic_baseline_format_quote_24,
                    KeyAction.QuickPhraseAction
                ),
                Popup.Menu.Item(
                    "Unicode",
                    R.drawable.ic_logo_unicode,
                    KeyAction.UnicodeAction
                )
            )
        )
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
        Behavior.LongPress(KeyAction.ShowInputMethodPickerAction)
    )
)

class SpaceKey : KeyDef(
    Appearance.Space,
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_space))),
        Behavior.LongPress(KeyAction.SpaceLongPressAction)
    )
)

class ReturnKey(percentWidth: Float = 0.15f) : KeyDef(
    Appearance.Return(percentWidth),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Return)))
    ),
    arrayOf(
        Popup.Menu(
            arrayOf(
                Popup.Menu.Item(
                    "Emoji", R.drawable.ic_baseline_tag_faces_24, KeyAction.PickerSwitchAction()
                )
            )
        )
    ),
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
        Behavior.Press(KeyAction.LayoutSwitchAction(to))
    )
)

class ImagePickerSwitchKey(
    @DrawableRes
    icon: Int,
    to: PickerWindow.Key,
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
        Behavior.Press(KeyAction.PickerSwitchAction(to))
    )
)

class TextPickerSwitchKey(
    text: String,
    to: PickerWindow.Key,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground
) : KeyDef(
    Appearance.TextLayoutSwitch(
        displayText = text,
        percentWidth = percentWidth,
        variant = variant,
    ),
    setOf(
        Behavior.Press(KeyAction.PickerSwitchAction(to))
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
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_space)))
    )
)

class NumPadSymbolKey(
    displayText: String,
    val sym: Int,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal
) : KeyDef(
    Appearance.Symbol(displayText, percentWidth, variant),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(sym), NumLockState))
    )
)

class NumPadNumberKey(
    displayText: String,
    val sym: Int,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal
) : KeyDef(
    Appearance.NumPadNum(displayText, percentWidth, variant),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(sym), NumLockState))
    )
)
