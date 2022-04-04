package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
import org.fcitx.fcitx5.android.utils.setupPressingToRepeat
import splitties.bitflags.hasFlag
import splitties.views.dsl.constraintlayout.*
import splitties.views.imageResource
import kotlin.math.roundToInt
import kotlin.math.sign

abstract class BaseKeyboard(
    context: Context,
    private val keyLayout: List<List<KeyDef>>
) : ConstraintLayout(context) {

    fun interface KeyActionListener {
        fun onKeyAction(action: KeyAction)
    }

    var keyActionListener: KeyActionListener? = null

    init {
        with(context) {
            val keyRows = keyLayout.map { row ->
                val keyViews = row.map { def ->
                    createKeyView(def)
                }
                constraintLayout Row@{
                    keyViews.forEachIndexed { index, view ->
                        addView(view, lParams {
                            topOfParent()
                            bottomOfParent()
                            if (index == 0) {
                                startOfParent()
                                horizontalChainStyle = LayoutParams.CHAIN_PACKED
                            } else after(keyViews[index - 1])
                            if (index == keyViews.size - 1) endOfParent()
                            else before(keyViews[index + 1])
                            val def = row[index]
                            matchConstraintPercentWidth = def.appearance.percentWidth
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

    protected fun createKeyView(def: KeyDef): KeyView {
        return when (def.appearance) {
            is KeyDef.Appearance.AltText -> AltTextKeyView(context, def.appearance)
            is KeyDef.Appearance.Text -> TextKeyView(context, def.appearance)
            is KeyDef.Appearance.Image -> ImageKeyView(context, def.appearance)
        }.apply {
            // set gestures
            if (def is SpaceKey) {
                setupOnGestureListener(object : MyOnGestureListener() {
                    var lastX = -1f
                    var lastT = -1L
                    override fun onRawTouchEvent(motionEvent: MotionEvent): Boolean {
                        if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                            if (lastX == -1f) {
                                lastX = motionEvent.x
                                lastT = System.currentTimeMillis()
                            }
                            val v =
                                (motionEvent.x - lastX) / ((System.currentTimeMillis() - lastT)
                                    .takeIf { it != 0L }
                                    ?: 1)
                            lastX = motionEvent.x
                            lastT = System.currentTimeMillis()
                            val times = (v * 5).roundToInt()
                            if (times != 0) {
                                val direction = times.sign
                                repeat(times / direction) {
                                    onAction(
                                        if (direction > 0)
                                            KeyAction.SymAction(KeySym.of(0xff53), KeyStates())
                                        else
                                            KeyAction.SymAction(KeySym.of(0xff51), KeyStates())
                                    )
                                }
                            }
                        }
                        return super.onRawTouchEvent(motionEvent)
                    }
                })
            }
            if (def.appearance is KeyDef.Appearance.AltText) {
                setupOnGestureListener(object : MyOnGestureListener() {
                    override fun onSwipeDown(displacement: Float, velocity: Float): Boolean {
                        // TODO make alt string a behavior
                        hapticIfEnabled()
                        onAction(KeyAction.FcitxKeyAction(def.appearance.altText))
                        return true
                    }
                })
            }
            def.behaviors.forEach {
                when (it) {
                    is KeyDef.Behavior.LongPress -> {
                        setOnLongClickListener { _ ->
                            onAction(it.action)
                            true
                        }
                    }
                    is KeyDef.Behavior.Press -> {
                        setOnClickListener { _ ->
                            hapticIfEnabled()
                            onAction(it.action)
                        }
                    }
                    is KeyDef.Behavior.Repeat -> {
                        setupPressingToRepeat { _ ->
                            onAction(it.action)
                        }
                    }
                }
            }
        }
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
    open fun onAction(action: KeyAction) {
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