package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.preedit.PreeditContent
import splitties.bitflags.hasFlag
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.imageResource

abstract class BaseKeyboard(
    context: Context,
    protected val theme: Theme,
    private val keyLayout: List<List<KeyDef>>
) : ConstraintLayout(context) {

    fun interface KeyActionListener {
        fun onKeyAction(action: KeyAction)
    }

    var keyActionListener: KeyActionListener? = null

    private val selectionSwipeThreshold = dp(10f)
    private val inputSwipeThreshold = dp(36f)

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

    private fun createKeyView(def: KeyDef): KeyView {
        return when (def.appearance) {
            is KeyDef.Appearance.AltText -> AltTextKeyView(context, theme, def.appearance)
            is KeyDef.Appearance.Text -> TextKeyView(context, theme, def.appearance)
            is KeyDef.Appearance.Image -> ImageKeyView(context, theme, def.appearance)
        }.apply {
            if (def is SpaceKey) {
                swipeEnabled = true
                swipeRepeatEnabled = true
                swipeThresholdX = selectionSwipeThreshold
                onSwipeLeftListener = { _, cnt ->
                    repeat(cnt) {
                        onAction(KeyAction.SymAction(KeySym(0xff51u), KeyStates()))
                    }
                }
                onSwipeRightListener = { _, cnt ->
                    repeat(cnt) {
                        onAction(KeyAction.SymAction(KeySym(0xff53u), KeyStates()))
                    }
                }
            } else if (def is BackspaceKey) {
                swipeEnabled = true
                swipeRepeatEnabled = true
                swipeThresholdX = selectionSwipeThreshold
                onSwipeLeftListener = { _, cnt ->
                    onAction(KeyAction.MoveSelectionAction(-1 * cnt))
                }
                onSwipeRightListener = { _, cnt ->
                    onAction(KeyAction.MoveSelectionAction(cnt))
                }
                onTouchLeaveListener = {
                    onAction(KeyAction.DeleteSelectionAction)
                }
            }
            def.behaviors.forEach {
                when (it) {
                    is KeyDef.Behavior.LongPress -> {
                        setOnLongClickListener { _ ->
                            onAction(it.action)
                            true
                        }
                    }
                    is KeyDef.Behavior.SwipeDown -> {
                        swipeEnabled = true
                        swipeThresholdY = inputSwipeThreshold
                        onSwipeDownListener = { _, _ ->
                            onAction(it.action)
                        }
                    }
                    is KeyDef.Behavior.DoubleTap -> {
                        doubleTapEnabled = true
                        onDoubleTapListener = { _ ->
                            onAction(it.action)
                        }
                    }
                    is KeyDef.Behavior.Press -> {
                        setOnClickListener { _ ->
                            onAction(it.action)
                        }
                    }
                    is KeyDef.Behavior.Repeat -> {
                        repeatEnabled = true
                        onRepeatListener = { _ ->
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