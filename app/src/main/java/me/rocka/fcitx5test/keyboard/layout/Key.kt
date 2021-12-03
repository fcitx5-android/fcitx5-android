package me.rocka.fcitx5test.keyboard.layout

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import me.rocka.fcitx5test.R

interface IKeyBehavior

interface IPressKey : IKeyBehavior {
    fun onPress(): KeyAction<*>
}

interface ILongPressKey : IPressKey {
    fun onLongPress(): KeyAction<*>
}

interface IKeyAppearance {
    val percentWidth: Float
}

interface ITextKey : IKeyAppearance {
    val displayText: String
}

interface IImageKey : IKeyAppearance {
    @get:DrawableRes
    val src: Int
}

interface ITintKey : IKeyAppearance {
    val foreground: Int
    val background: Int
}

interface IKeyProps

interface IKeyId : IKeyProps {
    @get:IdRes
    val id: Int
}

abstract class BaseKey(override val percentWidth: Float = 0.1f) :
    IKeyBehavior, IKeyAppearance, IKeyProps

open class TextKey(
    open val text: String,
    percentWidth: Float = 0.1f
) : BaseKey(percentWidth), ITextKey, IPressKey {
    override val displayText: String get() = text
    override fun onPress(): KeyAction<*> = KeyAction.FcitxKeyAction(text)
}

open class AltTextKey(
    override val text: String = "",
    private val altText: String = "",
    percentWidth: Float = 0.1f
) : TextKey(text, percentWidth), ILongPressKey {
    override val displayText: String get() = "$text\n$altText"
    override fun onLongPress(): KeyAction<*> = KeyAction.FcitxKeyAction(altText)
}

open class CapsKey(
    override val src: Int = R.drawable.ic_baseline_keyboard_capslock0_24,
    override val id: Int = R.id.button_caps
) : BaseKey(0.15f), IPressKey, IImageKey, IKeyId {
    override fun onPress() = KeyAction.CapsAction
}

open class BackspaceKey(
    override val src: Int = R.drawable.ic_baseline_backspace_24,
    override val id: Int = R.id.button_backspace
) : BaseKey(0.15f), IImageKey, IPressKey, IKeyId {
    override fun onPress() = KeyAction.BackspaceAction
}

open class QuickPhraseKey(
    override val src: Int = R.drawable.ic_baseline_format_quote_24
) : BaseKey(0.1f), IImageKey, ILongPressKey {
    override fun onPress() = KeyAction.QuickPhraseAction
    override fun onLongPress() = KeyAction.UnicodeAction
}

open class LangSwitchKey(
    override val src: Int = R.drawable.ic_baseline_language_24
) : BaseKey(), IImageKey, ILongPressKey {
    override fun onPress() = KeyAction.LangSwitchAction
    override fun onLongPress() = KeyAction.InputMethodSwitchAction
}

open class SpaceKey(
    percentWidth: Float = 0f,
    override val id: Int = R.id.button_space,
) : TextKey(" ", percentWidth), IKeyId

open class ReturnKey(
    override val src: Int = R.drawable.ic_baseline_keyboard_return_24,
    override val foreground: Int = android.R.attr.colorForegroundInverse,
    override val background: Int = R.attr.colorAccent
) : BaseKey(0.15f), IImageKey, IPressKey, ITintKey {
    override fun onPress() = KeyAction.ReturnAction
}

open class MiniSpaceKey(
    override val src: Int = R.drawable.ic_baseline_space_bar_24,
    override val id: Int = R.id.button_mini_space
) : BaseKey(0.15f), IImageKey, IPressKey, IKeyId {
    override fun onPress() = KeyAction.FcitxKeyAction(" ")
}

open class LayoutSwitchKey(
    override val src: Int = R.drawable.ic_baseline_keyboard_24,
    override val id: Int = R.id.button_layout_switch
) : BaseKey(0.15f) ,IImageKey, IPressKey, IKeyId {
    override fun onPress() = KeyAction.LayoutSwitchAction()
}
