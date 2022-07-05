package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.Rect
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.GestureType
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import splitties.bitflags.hasFlag
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.imageResource
import kotlin.math.absoluteValue

abstract class BaseKeyboard(
    context: Context,
    protected val theme: Theme,
    private val keyLayout: List<List<KeyDef>>
) : ConstraintLayout(context) {

    fun interface KeyActionListener {
        fun onKeyAction(action: KeyAction)
    }

    var keyActionListener: KeyActionListener? = null

    private var popupOnKeyPress by AppPrefs.getInstance().keyboard.popupOnKeyPress

    interface KeyPopupListener {
        fun onPreview(viewId: Int, content: String, bounds: Rect)
        fun onDismiss(viewId: Int)
        fun onShowKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect)
        fun onChangeFocus(viewId: Int, deltaX: Int, deltaY: Int): Boolean
        fun onKeyAction(viewId: Int): KeyAction?
    }

    var keyPopupListener: KeyPopupListener? = null

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
                onGestureListener = OnGestureListener { _, event ->
                    when (event.type) {
                        GestureType.Move -> {
                            val sym = if (event.countX > 0) 0xff53u else 0xff51u
                            val action = KeyAction.SymAction(KeySym(sym), KeyStates())
                            repeat(event.countX.absoluteValue) {
                                onAction(action)
                            }
                            true
                        }
                        else -> false
                    }
                }
            } else if (def is BackspaceKey) {
                swipeEnabled = true
                swipeRepeatEnabled = true
                swipeThresholdX = selectionSwipeThreshold
                onGestureListener = OnGestureListener { _, event ->
                    when (event.type) {
                        GestureType.Move -> {
                            onAction(KeyAction.MoveSelectionAction(event.countX))
                            true
                        }
                        GestureType.Up -> {
                            onAction(KeyAction.DeleteSelectionAction(event.totalX))
                            false
                        }
                        else -> false
                    }
                }
            }
            def.behaviors.forEach {
                when (it) {
                    is KeyDef.Behavior.Press -> {
                        setOnClickListener { _ ->
                            onAction(it.action)
                        }
                    }
                    is KeyDef.Behavior.LongPress -> {
                        setOnLongClickListener { _ ->
                            onAction(it.action)
                            true
                        }
                    }
                    is KeyDef.Behavior.Repeat -> {
                        repeatEnabled = true
                        onRepeatListener = { _ ->
                            onAction(it.action)
                        }
                    }
                    is KeyDef.Behavior.SwipeDown -> {
                        swipeEnabled = true
                        swipeThresholdY = inputSwipeThreshold
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        onGestureListener = OnGestureListener { view, event ->
                            when (event.type) {
                                GestureType.Up -> {
                                    if (event.totalY > 0) {
                                        onAction(it.action)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            } || oldOnGestureListener.onGesture(view, event)
                        }
                    }
                    is KeyDef.Behavior.DoubleTap -> {
                        doubleTapEnabled = true
                        onDoubleTapListener = { _ ->
                            onAction(it.action)
                        }
                    }
                }
            }
            def.popup?.forEach {
                when (it) {
                    is KeyDef.Popup.Keyboard -> {
                        setOnLongClickListener { view ->
                            view as KeyView
                            keyPopupListener?.onShowKeyboard(view.id, it, view.bounds)
                            // do not consume this LongClick gesture
                            false
                        }
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        swipeThresholdX = dp(38f)
                        swipeThresholdY = dp(48f)
                        onGestureListener = OnGestureListener { view, event ->
                            view as KeyView
                            when (event.type) {
                                GestureType.Move -> {
                                    keyPopupListener
                                        ?.onChangeFocus(view.id, event.countX, event.countY)
                                        ?: false
                                }
                                GestureType.Up -> {
                                    // ask popup keyboard whether there's a pending KeyAction
                                    keyPopupListener?.onKeyAction(view.id)?.let { act ->
                                        onAction(act)
                                        onPopupDismiss(view.id)
                                        true
                                    } ?: false
                                }
                                else -> false
                            } || oldOnGestureListener.onGesture(view, event)
                        }
                    }
                    is KeyDef.Popup.AltPreview -> {
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        onGestureListener = OnGestureListener { view, event ->
                            view as KeyView
                            when (event.type) {
                                GestureType.Down -> {
                                    onPopupPreview(view.id, it.content, view.bounds)
                                }
                                GestureType.Move -> {
                                    val text = if (event.totalY > 0) it.alternative else it.content
                                    onPopupPreview(view.id, text, view.bounds)
                                }
                                GestureType.Up -> {
                                    onPopupDismiss(view.id)
                                }
                            }
                            // never consume gesture in preview popup
                            oldOnGestureListener.onGesture(view, event)
                        }
                    }
                    is KeyDef.Popup.Preview -> {
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        onGestureListener = OnGestureListener { view, event ->
                            view as KeyView
                            when (event.type) {
                                GestureType.Down -> {
                                    onPopupPreview(view.id, it.content, view.bounds)
                                }
                                GestureType.Up -> {
                                    onPopupDismiss(view.id)
                                }
                                else -> {}
                            }
                            // never consume gesture in preview popup
                            oldOnGestureListener.onGesture(view, event)
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
        preedit: FcitxEvent.PreeditEvent.Data
        // aux: FcitxEvent.InputPanelAuxEvent.Data
    ) {
        val hasPreedit = preedit.preedit.isNotEmpty()
        // `auxUp` is not empty when switching input methods, ignore it to reduce flicker
        //        || aux.auxUp.isNotEmpty()
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

    @CallSuper
    open fun onPopupPreview(viewId: Int, content: String, bounds: Rect) {
        if (!popupOnKeyPress) return
        keyPopupListener?.onPreview(viewId, content, bounds)
    }

    @CallSuper
    open fun onPopupDismiss(viewId: Int) {
        keyPopupListener?.onDismiss(viewId)
    }

    open fun onAttach(info: EditorInfo? = null) {
        // do nothing by default
    }

    open fun onEditorInfoChange(info: EditorInfo?) {
        // do nothing by default
    }

    open fun onPreeditChange(info: EditorInfo?, data: FcitxEvent.PreeditEvent.Data) {
        // do nothing by default
    }

    open fun onInputMethodChange(ime: InputMethodEntry) {
        // do nothing by default
    }

    open fun onDetach() {
        // do nothing by default
    }

}