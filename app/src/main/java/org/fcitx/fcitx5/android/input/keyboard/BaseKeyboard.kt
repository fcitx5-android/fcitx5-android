package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
import splitties.bitflags.hasFlag
import splitties.views.dsl.constraintlayout.*
import splitties.views.imageResource

abstract class BaseKeyboard(
    context: Context,
    private val keyLayout: List<List<BaseKey>>
) : ConstraintLayout(context) {

    fun interface KeyActionListener {
        fun onKeyAction(action: KeyAction<*>)
    }

    var keyActionListener: KeyActionListener? = null

    init {
        with(context) {
            val keyRows = keyLayout.map { row ->
                val keyButtons = row.map { key ->
                    createKey(key)
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


    protected fun createKey(key: BaseKey): BaseKeyView =
        when (key) {
            is IImageKey -> ImageKeyView(context)
            is AltTextKey -> AltTextKeyView(context)
            else -> TextKeyView(context)
        }.apply {
            applyKey(key)
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
                    hapticIfEnabled()
                    return false
                }

                override fun onDoubleTap() = when (key) {
                    is IDoublePressKey -> {
                        hapticIfEnabled()
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
                            when (motionEvent.actionMasked) {
                                MotionEvent.ACTION_BUTTON_PRESS -> performClick()
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onAction(key.onRelease())
                            }
                        }
                    }
                    return false
                }
            })
        }


    @DrawableRes
    protected fun drawableForReturn(info: EditorInfo?): Int {
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

    // FIXME: need some new API to know exactly whether next enter would be captured by fcitx
    protected fun updateReturnButton(
        `return`: ImageKeyView,
        info: EditorInfo?,
        content: PreeditContent
    ) {
        val hasPreedit = content.preedit.preedit.isNotEmpty()
        // `auxUp` is not empty when switching input methods, ignore it to reduce flicker
        //        || content.aux.auxUp.isNotEmpty()
        `return`.img.imageResource = if (hasPreedit) {
            R.drawable.ic_baseline_keyboard_return_24
        } else {
            drawableForReturn(info)
        }
    }

    @CallSuper
    open fun onAction(action: KeyAction<*>) {
        keyActionListener?.onKeyAction(action)
    }

    open fun onAttach(info: EditorInfo? = null) {
        // do nothing by default
    }

    open fun onEditorInfoChange(info: EditorInfo?) {
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