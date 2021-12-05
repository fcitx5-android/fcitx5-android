package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.keyboard.layout.*
import me.rocka.fcitx5test.native.InputMethodEntry
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.button
import splitties.views.dsl.core.imageButton
import splitties.views.imageResource
import splitties.views.padding

abstract class BaseKeyboard(
    context: Context,
    private val keyLayout: List<List<BaseKey>>
) : ConstraintLayout(context) {

    private var passAction: ((View, KeyAction<*>, Boolean) -> Unit)? = null

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

    fun setOnKeyActionListener(f: ((View, KeyAction<*>, Boolean) -> Unit)?) {
        passAction = f
    }

    @CallSuper
    open fun onAction(view: View, action: KeyAction<*>, long: Boolean) {
        passAction?.invoke(view, action, long)
    }

    open fun onAttach() {
        // do nothing by default
    }

    open fun onInputMethodChange(ime: InputMethodEntry) {
        // do nothing by default
    }

    open fun onDetach() {
        // do nothing by default
    }

}