package me.rocka.fcitx5test.keyboard.layout

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import me.rocka.fcitx5test.R

interface IKeyBehavior

interface IPressKey : IKeyBehavior {
    fun onPress(): ButtonAction<*>
}

interface ILongPressKey : IPressKey {
    fun onLongPress(): ButtonAction<*>
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

abstract class BaseButton(override val percentWidth: Float = 0.1f) :
    IKeyBehavior, IKeyAppearance, IKeyProps

open class TextButton(
    open val text: String,
    percentWidth: Float = 0.1f
) : BaseButton(percentWidth), ITextKey, IPressKey {
    override val displayText: String get() = text
    override fun onPress(): ButtonAction<*> = ButtonAction.FcitxKeyAction(text)
}

open class AltTextButton(
    override val text: String = "",
    private val altText: String = "",
    percentWidth: Float = 0.1f
) : TextButton(text, percentWidth), ILongPressKey {
    override val displayText: String get() = "$text\n$altText"
    override fun onLongPress(): ButtonAction<*> = ButtonAction.FcitxKeyAction(altText)
}

open class CapsButton(
    override val src: Int = R.drawable.ic_baseline_keyboard_capslock0_24,
    override val id: Int = R.id.button_caps
) : BaseButton(0.15f), IPressKey, IImageKey, IKeyId {
    override fun onPress() = ButtonAction.CapsAction
}

open class BackspaceButton(
    override val src: Int = R.drawable.ic_baseline_backspace_24,
    override val id: Int = R.id.button_backspace
) : BaseButton(0.15f), IImageKey, IPressKey, IKeyId {
    override fun onPress() = ButtonAction.BackspaceAction
}

open class QuickPhraseButton(
    override val src: Int = R.drawable.ic_baseline_format_quote_24
) : BaseButton(0.1f), IImageKey, ILongPressKey {
    override fun onPress() = ButtonAction.QuickPhraseAction
    override fun onLongPress() = ButtonAction.UnicodeAction
}

open class LangSwitchButton(
    override val src: Int = R.drawable.ic_baseline_language_24
) : BaseButton(), IImageKey, ILongPressKey {
    override fun onPress() = ButtonAction.LangSwitchAction
    override fun onLongPress() = ButtonAction.InputMethodSwitchAction
}

open class SpaceButton(
    percentWidth: Float = 0f,
    override val id: Int = R.id.button_space,
) : TextButton(" ", percentWidth), IKeyId

open class ReturnButton(
    override val src: Int = R.drawable.ic_baseline_keyboard_return_24,
    override val foreground: Int = android.R.attr.colorForegroundInverse,
    override val background: Int = R.attr.colorAccent
) : BaseButton(0.15f), IImageKey, IPressKey, ITintKey {
    override fun onPress() = ButtonAction.ReturnAction
}

open class MiniSpaceButton(
    override val src: Int = R.drawable.ic_baseline_space_bar_24,
    override val id: Int = R.id.button_mini_space
) : BaseButton(0.15f), IImageKey, IPressKey, IKeyId {
    override fun onPress() = ButtonAction.FcitxKeyAction(" ")
}

open class LayoutSwitchButton(
    override val src: Int = R.drawable.ic_baseline_keyboard_24,
    override val id: Int = R.id.button_layout_switch
) : BaseButton(0.15f) ,IImageKey, IPressKey, IKeyId {
    override fun onPress() = ButtonAction.LayoutSwitchAction()
}
