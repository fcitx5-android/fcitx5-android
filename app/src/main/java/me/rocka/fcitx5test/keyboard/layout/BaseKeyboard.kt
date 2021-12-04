package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.AppSharedPreferences
import me.rocka.fcitx5test.keyboard.layout.*
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.FcitxEvent
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.button
import splitties.views.dsl.core.imageButton
import splitties.views.imageResource
import splitties.views.padding
import java.util.*
import kotlin.concurrent.timer

abstract class BaseKeyboard(
    context: Context,
    private val fcitx: Fcitx,
    private val keyLayout: List<List<BaseKey>>,
    private val passAction: (View, KeyAction<*>, Boolean) -> Unit
) : ConstraintLayout(context) {

    private var backspaceTimer: Timer? = null

    init {
        with(context) {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            val keyRows = keyLayout.map { row ->
                val keyButtons = row.map { key ->
                    createButton(context, key).apply {
                        if (key is IPressKey) setOnClickListener {
                            onAction(this, key.onPress(), false)
                        }
                        if (key is ILongPressKey) setOnLongClickListener {
                            onAction(this, key.onLongPress(), true)
                            true
                        }
                    }
                }
                constraintLayout Row@{
                    keyButtons.forEachIndexed { index, button ->
                        addView(button, lParams {
                            topOfParent()
                            bottomOfParent()
                            if (index == 0) {
                                startOfParent()
                                horizontalChainStyle = LayoutParams.CHAIN_PACKED
                            } else after(keyButtons[index - 1])
                            if (index == keyButtons.size - 1) endOfParent()
                            else before(keyButtons[index + 1])
                            val buttonDef = row[index]
                            matchConstraintPercentWidth = buttonDef.percentWidth
                        })
                    }
                }
            }
            keyRows.forEachIndexed { index, row ->
                addView(row, lParams {
                    height = dp(60)
                    if (index == 0) topOfParent()
                    else below(keyRows[index - 1])
                    if (index == keyRows.size - 1) bottomOfParent()
                    else above(keyRows[index + 1])
                    startOfParent()
                    endOfParent()
                })
            }
        }
    }

    private fun createButton(context: Context, btn: BaseKey): View = with(context) {
        when (btn) {
            is IImageKey -> imageButton {
                imageResource = btn.src
                if (btn is ITintKey) {
                    backgroundTintList = styledColorSL(btn.background)
                    colorFilter =
                        PorterDuffColorFilter(styledColor(btn.foreground), PorterDuff.Mode.SRC_IN)
                }
            }
            is ITextKey -> button {
                text = btn.displayText
                textSize = 16f // sp
                isAllCaps = false
            }
            else -> button {}
        }.apply {
            when (btn) {
                is IKeyId -> {
                    id = btn.id
                }
            }
            padding = 0
            elevation = dp(2f)
        }
    }

    abstract fun handleFcitxEvent(event: FcitxEvent<*>)

    @CallSuper
    open fun onAction(v: View, it: KeyAction<*>, long: Boolean) {
        if (AppSharedPreferences.getInstance().buttonHapticFeedback && (!long)) {
            // TODO: write our own button to handle haptic feedback for both tap and long click
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        passAction(v, it, long)
    }

    open fun onKeyPress(key: String) {
        fcitx.sendKey(key)
    }

    fun backspace() {
        fcitx.sendKey("BackSpace")
    }

    fun startDeleting() {
        backspaceTimer = timer(period = 60L, action = { backspace() })
    }

    fun stopDeleting() {
        backspaceTimer?.run { cancel(); purge() }
    }

    fun quickPhrase() {
        fcitx.reset()
        fcitx.triggerQuickPhrase()
    }

    fun unicode() {
        fcitx.triggerUnicode()
    }

    fun switchLang() {
        val list = fcitx.enabledIme()
        if (list.isEmpty()) return
        val status = fcitx.ime()
        val index = list.indexOfFirst { it.uniqueName == status.uniqueName }
        val next = list[(index + 1) % list.size]
        fcitx.activateIme(next.uniqueName)
    }

    fun space() {
        fcitx.sendKey("space")
    }

    fun enter() {
        fcitx.sendKey("Return")
    }

    fun customEvent(fn: (Fcitx) -> Unit) {
        fn(fcitx)
    }

}