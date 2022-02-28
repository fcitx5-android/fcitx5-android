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
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.data.Prefs
import me.rocka.fcitx5test.keyboard.PreeditContent
import splitties.bitflags.hasFlag
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledColorSL
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
        fun onKeyAction(action: KeyAction<*>)
    }

    var keyActionListener: KeyActionListener? = null

    private val buttonHapticFeedback by Prefs.getInstance().buttonHapticFeedback

    init {
        with(context) {
            val keyRows = keyLayout.map { row ->
                val keyButtons = row.map { key ->
                    createButton(context, key).apply Button@{
                        setOnClickListener {
                            if (key is IPressKey)
                                onAction(key.onPress())
                        }
                        setOnLongClickListener {
                            when (key) {
                                is ILongPressKey -> {
                                    onAction(key.onLongPress())
                                    true
                                }
                                is IRepeatKey -> {
                                    onAction(key.onHold())
                                    true
                                }
                                else -> false
                            }
                        }
                        setupOnGestureListener(object : MyOnGestureListener() {
                            override fun onDown(e: MotionEvent?): Boolean {
                                haptic()
                                return false
                            }

                            override fun onDoubleTap() = when (key) {
                                is IDoublePressKey -> {
                                    haptic()
                                    onAction(key.onDoublePress())
                                    true
                                }
                                else -> false
                            }

                            override fun onSwipeDown() = when (key) {
                                is ILongPressKey -> {
                                    onAction(key.onLongPress())
                                    true
                                }
                                else -> false
                            }

                            override fun onRawTouchEvent(motionEvent: MotionEvent): Boolean {
                                when (key) {
                                    is IRepeatKey -> {
                                        when (motionEvent.action) {
                                            MotionEvent.ACTION_BUTTON_PRESS -> this@Button.performClick()
                                            MotionEvent.ACTION_UP -> onAction(key.onRelease())
                                        }
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
            isHapticFeedbackEnabled = false
        }
    }

    fun haptic() {
        if (buttonHapticFeedback)
            performHapticFeedback(
                KEYBOARD_TAP,
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
    open fun onAction(action: KeyAction<*>) {
        keyActionListener?.onKeyAction(action)
    }

    open fun onAttach(info: EditorInfo? = null) {
        // do nothing by default
    }

    open fun onPreeditChange(info: EditorInfo?, content: PreeditContent) {
        // do nothing by default
    }

    open fun onInputMethodChange(ime: InputMethodEntry) {
        // do nothing by default
    }

    open fun onDetach() {
        // do nothing by default
    }

}