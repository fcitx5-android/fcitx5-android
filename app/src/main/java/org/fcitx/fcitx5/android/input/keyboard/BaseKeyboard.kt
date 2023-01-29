package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.*
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.GestureType
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.input.popup.PopupListener
import splitties.bitflags.hasFlag
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.imageResource
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

abstract class BaseKeyboard(
    context: Context,
    protected val theme: Theme,
    private val keyLayout: List<List<KeyDef>>
) : ConstraintLayout(context) {

    var keyActionListener: KeyActionListener? = null

    private val popupOnKeyPress by AppPrefs.getInstance().keyboard.popupOnKeyPress
    private val swipeSymbolDirection by AppPrefs.getInstance().keyboard.swipeSymbolDirection

    private val vivoKeypressWorkaround by AppPrefs.getInstance().advanced.vivoKeypressWorkaround

    var keyPopupListener: PopupListener? = null

    private val selectionSwipeThreshold = dp(10f)
    private val inputSwipeThreshold = dp(36f)

    private val bounds = Rect()
    private val keyRows: List<ConstraintLayout>

    /**
     * HashMap of [PointerId (Int)][MotionEvent.getPointerId] and [KeyView]
     */
    private val touchTarget = hashMapOf<Int, View>()

    init {
        keyRows = keyLayout.map { row ->
            val keyViews = row.map { def ->
                createKeyView(def)
            }
            constraintLayout Row@{
                keyViews.forEachIndexed { index, view ->
                    add(view, lParams {
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
            add(row, lParams {
                if (index == 0) topOfParent()
                else below(keyRows[index - 1])
                if (index == keyRows.size - 1) bottomOfParent()
                else above(keyRows[index + 1])
                startOfParent()
                endOfParent()
            })
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
                        GestureType.Move -> when (val count = event.countX) {
                            0 -> false
                            else -> {
                                val sym =
                                    if (count > 0) FcitxKeyMapping.FcitxKey_Right else FcitxKeyMapping.FcitxKey_Left
                                val action = KeyAction.SymAction(KeySym(sym), KeyStates.Empty)
                                repeat(count.absoluteValue) {
                                    onAction(action)
                                }
                                true
                            }
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
                            val count = event.countX
                            if (count != 0) {
                                onAction(KeyAction.MoveSelectionAction(count))
                                true
                            } else false
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
                    is KeyDef.Behavior.Swipe -> {
                        swipeEnabled = true
                        swipeThresholdY = inputSwipeThreshold
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        onGestureListener = OnGestureListener { view, event ->
                            when (event.type) {
                                GestureType.Up -> {
                                    if (!event.consumed && swipeSymbolDirection.checkY(event.totalY)) {
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
                    // TODO: gesture processing middleware
                    is KeyDef.Popup.Menu -> {
                        setOnLongClickListener { view ->
                            view as KeyView
                            onPopupMenu(view.id, it, view.bounds)
                            // do not consume this LongClick gesture
                            false
                        }
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        swipeEnabled = true
                        onGestureListener = OnGestureListener { view, event ->
                            view as KeyView
                            when (event.type) {
                                GestureType.Move -> {
                                    onPopupChangeFocus(view.id, event.x, event.y)
                                }
                                GestureType.Up -> {
                                    onPopupTrigger(view.id)
                                }
                                else -> false
                            } || oldOnGestureListener.onGesture(view, event)
                        }
                    }
                    is KeyDef.Popup.Keyboard -> {
                        setOnLongClickListener { view ->
                            view as KeyView
                            onPopupKeyboard(view.id, it, view.bounds)
                            // do not consume this LongClick gesture
                            false
                        }
                        val oldOnGestureListener = onGestureListener ?: OnGestureListener.Empty
                        swipeEnabled = true
                        onGestureListener = OnGestureListener { view, event ->
                            view as KeyView
                            when (event.type) {
                                GestureType.Move -> {
                                    onPopupChangeFocus(view.id, event.x, event.y)
                                }
                                GestureType.Up -> {
                                    onPopupTrigger(view.id)
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
                                    val triggered = swipeSymbolDirection.checkY(event.totalY)
                                    val text = if (triggered) it.alternative else it.content
                                    onPopupPreviewUpdate(view.id, text)
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
        val hasPreedit = preedit.preedit.isNotEmpty() || preedit.clientPreedit.isNotEmpty()
        // `auxUp` is not empty when switching input methods, ignore it to reduce flicker
        //        || aux.auxUp.isNotEmpty()
        `return`.img.imageResource = if (hasPreedit) {
            R.drawable.ic_baseline_keyboard_return_24
        } else {
            drawableForReturn(info)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val (x, y) = intArrayOf(0, 0).also { getLocationInWindow(it) }
        bounds.set(x, y, x + width, y + height)
    }

    private fun findTargetChild(x: Float, y: Float): View? {
        val y0 = y.roundToInt()
        // assume all rows have equal height
        val row = keyRows.getOrNull(y0 * keyRows.size / bounds.height()) ?: return null
        val x1 = x.roundToInt() + bounds.left
        val y1 = y0 + bounds.top
        return row.children.find {
            if (it !is KeyView) false else it.bounds.contains(x1, y1)
        }
    }

    private fun transformMotionEventToChild(
        child: View,
        event: MotionEvent,
        action: Int,
        pointerIndex: Int
    ): MotionEvent {
        if (child !is KeyView) {
            Timber.w("child view is not KeyView when transforming MotionEvent $event")
            return event
        }
        val childX = event.getX(pointerIndex) + bounds.left - child.bounds.left
        val childY = event.getY(pointerIndex) + bounds.top - child.bounds.top
        return MotionEvent.obtain(
            event.downTime, event.eventTime, action,
            childX, childY, event.getPressure(pointerIndex), event.getSize(pointerIndex),
            event.metaState, event.xPrecision, event.yPrecision,
            event.deviceId, event.edgeFlags
        )
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // intercept ACTION_DOWN and all following events will go to parent's onTouchEvent
        return if (vivoKeypressWorkaround && ev.actionMasked == MotionEvent.ACTION_DOWN) true
        else super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (vivoKeypressWorkaround) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val target = findTargetChild(event.x, event.y) ?: return false
                    touchTarget[event.getPointerId(0)] = target
                    target.dispatchTouchEvent(
                        transformMotionEventToChild(target, event, MotionEvent.ACTION_DOWN, 0)
                    )
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val i = event.actionIndex
                    val target = findTargetChild(event.getX(i), event.getY(i)) ?: return false
                    touchTarget[event.getPointerId(i)] = target
                    target.dispatchTouchEvent(
                        transformMotionEventToChild(target, event, MotionEvent.ACTION_DOWN, i)
                    )
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val target = touchTarget[event.getPointerId(i)] ?: continue
                        target.dispatchTouchEvent(
                            transformMotionEventToChild(target, event, MotionEvent.ACTION_MOVE, i)
                        )
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val i = event.actionIndex
                    val pid = event.getPointerId(i)
                    val target = touchTarget[event.getPointerId(i)] ?: return false
                    target.dispatchTouchEvent(
                        transformMotionEventToChild(target, event, MotionEvent.ACTION_UP, i)
                    )
                    touchTarget.remove(pid)
                    return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val i = event.actionIndex
                    val pid = event.getPointerId(i)
                    val target = touchTarget[event.getPointerId(i)] ?: return false
                    target.dispatchTouchEvent(
                        transformMotionEventToChild(target, event, MotionEvent.ACTION_UP, i)
                    )
                    touchTarget.remove(pid)
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    val i = event.actionIndex
                    val pid = event.getPointerId(i)
                    val target = touchTarget[pid] ?: return false
                    target.dispatchTouchEvent(
                        transformMotionEventToChild(target, event, MotionEvent.ACTION_CANCEL, i)
                    )
                    touchTarget.remove(pid)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    @CallSuper
    open fun onAction(
        action: KeyAction,
        source: KeyActionListener.Source = KeyActionListener.Source.Keyboard
    ) {
        keyActionListener?.onKeyAction(action, source)
    }

    @CallSuper
    open fun onPopupPreview(viewId: Int, content: String, bounds: Rect) {
        if (!popupOnKeyPress) return
        keyPopupListener?.onPreview(viewId, content, bounds)
    }

    @CallSuper
    open fun onPopupPreviewUpdate(viewId: Int, content: String) {
        if (!popupOnKeyPress) return
        keyPopupListener?.onPreviewUpdate(viewId, content)
    }

    @CallSuper
    open fun onPopupDismiss(viewId: Int) {
        keyPopupListener?.onDismiss(viewId)
    }

    @CallSuper
    open fun onPopupKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect) {
        keyPopupListener?.onShowKeyboard(viewId, keyboard, bounds)
    }

    open fun onPopupMenu(viewId: Int, menu: KeyDef.Popup.Menu, bounds: Rect) {
        keyPopupListener?.onShowMenu(viewId, menu, bounds)
    }

    @CallSuper
    open fun onPopupChangeFocus(viewId: Int, x: Float, y: Float): Boolean {
        return keyPopupListener?.onChangeFocus(viewId, x, y) ?: false
    }

    @CallSuper
    fun onPopupTrigger(viewId: Int): Boolean {
        // ask popup keyboard whether there's a pending KeyAction
        val action = keyPopupListener?.onTrigger(viewId) ?: return false
        onAction(action, KeyActionListener.Source.Popup)
        onPopupDismiss(viewId)
        return true
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

    open fun onPunctuationUpdate(mapping: Map<String, String>) {
        // do nothing by default
    }

    open fun onInputMethodChange(ime: InputMethodEntry) {
        // do nothing by default
    }

    open fun onDetach() {
        // do nothing by default
    }

}