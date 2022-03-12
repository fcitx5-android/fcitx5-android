package me.rocka.fcitx5test.input.keyboard

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

interface IDoublePressKey : IPressKey {
    fun onDoublePress(): KeyAction<*>
}

interface IRepeatKey : IKeyBehavior {
    fun onHold(): KeyAction<*>
    fun onRelease(): KeyAction<*>
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

open class KPKey(
    open val key: String,
    override val displayText: String,
    percentWidth: Float = 0.1f
) : BaseKey(percentWidth), ITextKey, IPressKey {
    constructor(key: String, percentWidth: Float) : this(key, key, percentWidth)

    override fun onPress(): KeyAction<*> = KeyAction.FcitxKeyAction("KP_$key")
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
) : BaseKey(0.15f), IDoublePressKey, IImageKey, IKeyId {
    override fun onPress() = KeyAction.CapsAction(false)
    override fun onDoublePress() = KeyAction.CapsAction(true)
}

open class BackspaceKey(
    override val src: Int = R.drawable.ic_baseline_backspace_24,
    override val id: Int = R.id.button_backspace
) : BaseKey(0.15f), IImageKey, IPressKey, IRepeatKey, IKeyId {
    override fun onPress() = KeyAction.FcitxKeyAction("BackSpace")
    override fun onHold() = KeyAction.RepeatStartAction("BackSpace")
    override fun onRelease() = KeyAction.RepeatEndAction("BackSpace")
}

open class QuickPhraseKey(
    override val src: Int = R.drawable.ic_baseline_format_quote_24,
    override val id: Int = R.id.button_quickphrase
) : BaseKey(0.1f), IImageKey, ILongPressKey, IKeyId {
    override fun onPress() = KeyAction.QuickPhraseAction
    override fun onLongPress() = KeyAction.UnicodeAction
}

open class LangSwitchKey(
    override val src: Int = R.drawable.ic_baseline_language_24,
    override val id: Int = R.id.button_lang
) : BaseKey(), IImageKey, ILongPressKey, IKeyId {
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
    override val background: Int = android.R.attr.colorAccent,
    override val id: Int = R.id.button_return
) : BaseKey(0.15f), IImageKey, IPressKey, ITintKey, IKeyId {
    override fun onPress() = KeyAction.FcitxKeyAction("Return")
}

open class MiniSpaceKey(
    override val src: Int = R.drawable.ic_baseline_space_bar_24,
    override val id: Int = R.id.button_mini_space
) : BaseKey(0.15f), IImageKey, IPressKey, IKeyId {
    override fun onPress() = KeyAction.FcitxKeyAction(" ")
}

open class LayoutSwitchKey(
    override val displayText: String,
    val to: String,
    override val percentWidth: Float = 0.15f,
) : BaseKey(percentWidth), ITextKey, IPressKey {
    override fun onPress() = KeyAction.LayoutSwitchAction(to)
}

open class ImageLayoutSwitchKey(
    override val src: Int,
    val to: String,
    override val percentWidth: Float,
) : BaseKey(percentWidth), IImageKey, IPressKey {
    override fun onPress() = KeyAction.LayoutSwitchAction(to)
}
