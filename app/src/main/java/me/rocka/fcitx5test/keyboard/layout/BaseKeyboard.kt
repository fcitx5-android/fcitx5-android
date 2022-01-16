package me.rocka.fcitx5test.keyboard.layout

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.HapticFeedbackConstants.*
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.keyboard.InputView
import me.rocka.fcitx5test.keyboard.layout.*
import me.rocka.fcitx5test.native.InputMethodEntry
import splitties.bitflags.hasFlag
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

    fun interface KeyActionListener {
        fun onKeyAction(view: View, action: KeyAction<*>)
    }

    var keyActionListener: KeyActionListener? = null

    init {
        with(context) {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            val keyRows = keyLayout.map { row ->
                val keyButtons = row.map { key ->
                    createButton(context, key).apply {
                        setOnClickListener {
                            haptic(false)
                            if (key is IPressKey)
                                onAction(it, key.onPress())
                        }
                        setOnLongClickListener {
                            haptic(true)
                            when (key) {
                                is ILongPressKey -> {
                                    onAction(it, key.onLongPress())
                                    true
                                }
                                is IRepeatKey -> {
                                    onAction(it, key.onHold())
                                    true
                                }
                                else -> false
                            }
                        }
                        setupOnGestureListener(object : MyOnGestureListener() {

                            override fun onDoubleTap(): Boolean {
                                return if (key is CapsKey) {
                                    onAction(this@apply, KeyAction.CapsAction(true))
                                    true
                                } else false
                            }

                            override fun onSwipeDown(): Boolean {
                                return if (key is AltTextKey) {
                                    haptic(false)
                                    onAction(this@apply, key.onLongPress())
                                    true
                                } else false
                            }

                            override fun onRawTouchEvent(motionEvent: MotionEvent): Boolean {
                                if (key is IRepeatKey) {
                                    when (motionEvent.action) {
                                        MotionEvent.ACTION_BUTTON_PRESS -> this@apply.performClick()
                                        MotionEvent.ACTION_UP -> onAction(
                                            this@apply,
                                            key.onRelease()
                                        )
                                    }
                                }
                                return false
                            }
                        })
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

    private fun haptic(long: Boolean) {

        if (Prefs.getInstance().buttonHapticFeedback)
            performHapticFeedback(
                if (!long)
                    KEYBOARD_TAP else LONG_PRESS,
                FLAG_IGNORE_GLOBAL_SETTING or FLAG_IGNORE_VIEW_SETTING
            )
    }

    @DrawableRes
    fun drawableForReturn(info: EditorInfo?): Int {
        if (info?.imeOptions?.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION) == true) {
            return R.drawable.ic_baseline_keyboard_return_24
        }
        return when (info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_GO -> R.drawable.ic_baseline_arrow_forward_24
            EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_baseline_search_24
            EditorInfo.IME_ACTION_SEND -> R.drawable.ic_baseline_send_24
            EditorInfo.IME_ACTION_NEXT -> R.drawable.ic_baseline_keyboard_tab_24
            EditorInfo.IME_ACTION_DONE -> R.drawable.ic_baseline_done_24
            EditorInfo.IME_ACTION_PREVIOUS -> R.drawable.ic_baseline_keyboard_tab_reverse_24
            else -> R.drawable.ic_baseline_keyboard_return_24
        }
    }

    @CallSuper
    open fun onAction(view: View, action: KeyAction<*>) {
        keyActionListener?.onKeyAction(view, action)
    }

    open fun onAttach(info: EditorInfo? = null) {
        // do nothing by default
    }

    open fun onPreeditChange(info: EditorInfo?, content: InputView.PreeditContent) {
        // do nothing by default
    }

    open fun onInputMethodChange(ime: InputMethodEntry) {
        // do nothing by default
    }

    open fun onDetach() {
        // do nothing by default
    }

}