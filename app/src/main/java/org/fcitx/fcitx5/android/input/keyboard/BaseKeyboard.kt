/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.allViews
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.GestureType
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.leftToRightOf
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.constraintlayout.rightToLeftOf
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.startToEndOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

abstract class BaseKeyboard(
    context: Context,
    protected val theme: Theme,
    private val keyLayout: List<List<KeyDef>>
) : ConstraintLayout(context) {

    var keyActionListener: KeyActionListener? = null

    protected open val supportsSplitLayout: Boolean = true

    private val prefs = AppPrefs.getInstance()

    private val popupOnKeyPress by prefs.keyboard.popupOnKeyPress
    private val expandKeypressArea by prefs.keyboard.expandKeypressArea
    private val swipeSymbolDirection by prefs.keyboard.swipeSymbolDirection

    private val spaceSwipeMoveCursor = prefs.keyboard.spaceSwipeMoveCursor
    private val spaceKeys = mutableListOf<KeyView>()
    private val spaceSwipeChangeListener = ManagedPreference.OnChangeListener<Boolean> { _, v ->
        spaceKeys.forEach {
            it.swipeEnabled = v
        }
    }
    private val splitKeyboard = prefs.keyboard.splitKeyboard
    private val splitKeyboardListener = ManagedPreference.OnChangeListener<Boolean> { _, v ->
        rebuildKeyboardRows(v)
    }
    private val splitThresholdListener = ManagedPreference.OnChangeListener<Float> { _, _ ->
        rebuildKeyboardRows(splitKeyboard.getValue())
    }
    private val splitBlankRatioListener = ManagedPreference.OnChangeListener<Int> { _, _ ->
        rebuildKeyboardRows(splitKeyboard.getValue())
    }

    private val vivoKeypressWorkaround by prefs.advanced.vivoKeypressWorkaround

    private val hapticOnRepeat by prefs.keyboard.hapticOnRepeat

    var popupActionListener: PopupActionListener? = null

    private val selectionSwipeThreshold = dp(10f)
    private val inputSwipeThreshold = dp(36f)

    // a rather large threshold effectively disables swipe of the direction
    private val disabledSwipeThreshold = dp(800f)

    private val bounds = Rect()
    private var keyRows: List<ConstraintLayout> = emptyList()
    private var lastInputMethod: InputMethodEntry? = null
    private var lastSplitAllowed = true
    private var lastSplitRequested = false
    private var layoutCallbacksEnabled = false
    private var lastMeasuredWidth = 0
    private var lastMeasuredHeight = 0

    private var isSplitLayout = false

    /**
     * HashMap of [PointerId (Int)][MotionEvent.getPointerId] to [KeyView]
     */
    private val touchTarget = hashMapOf<Int, View>()

    init {
        isMotionEventSplittingEnabled = true
        rebuildKeyboardRows(splitKeyboard.getValue())
        spaceSwipeMoveCursor.registerOnChangeListener(spaceSwipeChangeListener)
        splitKeyboard.registerOnChangeListener(splitKeyboardListener)
        prefs.keyboard.splitKeyboardThreshold.registerOnChangeListener(splitThresholdListener)
        prefs.keyboard.splitKeyboardBlankRatio.registerOnChangeListener(splitBlankRatioListener)
        prefs.keyboard.splitKeyboardBlankRatioLandscape.registerOnChangeListener(splitBlankRatioListener)
    }

    private fun rebuildKeyboardRows(split: Boolean) {
        val effectiveSplit = split && supportsSplitLayout && isSplitAllowed()
        if (isSplitLayout == effectiveSplit && lastSplitRequested == split && keyRows.isNotEmpty()) return
        isSplitLayout = effectiveSplit
        lastSplitRequested = split
        spaceKeys.clear()
        removeAllViews()
        val gapRatio = splitGapRatio()
        val rowGroupPercents = if (effectiveSplit) {
            val ratio = (1f - gapRatio).coerceAtLeast(0f)
            val nonSpacePercents = keyLayout.mapIndexed { _, row ->
                if (rowContainsSpaceKey(row)) 0f
                else {
                    val keyCount = row.size
                    if (keyCount <= 0) return@mapIndexed 0f
                    val oddAdjust = if (keyCount % 2 == 0 || !isStandardWidthRow(row)) {
                        1f
                    } else {
                        keyCount.toFloat() / (keyCount + 1f)
                    }
                    ratio * oddAdjust
                }
            }
            nonSpacePercents.mapIndexed { index, percent ->
                if (percent > 0f) percent
                else {
                    val prev = nonSpacePercents.getOrNull(index - 1) ?: 0f
                    val next = nonSpacePercents.getOrNull(index + 1) ?: 0f
                    maxOf(prev, next, ratio)
                }
            }
        } else {
            emptyList()
        }
        keyRows = keyLayout.mapIndexed { index, row ->
            if (effectiveSplit) {
                if (rowContainsSpaceKey(row)) {
                    createSpaceSplitRow(row, gapRatio)
                } else {
                    val groupPercent = rowGroupPercents.getOrNull(index) ?: 1f
                    createSplitRowWithGap(row, gapRatio, groupPercent)
                }
            } else {
                createKeyRow(row)
            }
        }
        keyRows.forEachIndexed { index, row ->
            if (effectiveSplit) {
                add(row, lParams(matchConstraints, matchConstraints) {
                    if (index == 0) topOfParent()
                    else below(keyRows[index - 1])
                    if (index == keyRows.size - 1) bottomOfParent()
                    else above(keyRows[index + 1])
                    startOfParent()
                    endOfParent()
                    matchConstraintDefaultHeight = LayoutParams.MATCH_CONSTRAINT_SPREAD
                })
            } else {
                add(row, lParams {
                    if (index == 0) topOfParent()
                    else below(keyRows[index - 1])
                    if (index == keyRows.size - 1) bottomOfParent()
                    else above(keyRows[index + 1])
                    centerHorizontally()
                })
            }
        }
        lastInputMethod?.let { onInputMethodUpdate(it) }
        if (layoutCallbacksEnabled) {
            onKeyboardLayoutRebuilt()
        }
        Timber.d("rebuildKeyboardRows split=%s rows=%d", effectiveSplit, keyRows.size)
    }

    private fun rowContainsSpaceKey(row: List<KeyDef>): Boolean {
        return row.any { it is SpaceKey || it is MiniSpaceKey }
    }

    private fun splitGapRatio(): Float {
        val percent = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            prefs.keyboard.splitKeyboardBlankRatioLandscape.getValue()
        } else {
            prefs.keyboard.splitKeyboardBlankRatio.getValue()
        }
        return (percent / 100f).coerceIn(0f, 0.9f)
    }

    private fun isSplitAllowed(width: Int = this.width, height: Int = this.height): Boolean {
        val w = if (width > 0) width else lastMeasuredWidth
        val h = if (height > 0) height else lastMeasuredHeight
        if (h <= 0) return false
        val threshold = prefs.keyboard.splitKeyboardThreshold.getValue()
        return (w.toFloat() / h.toFloat()) > threshold
    }

    private fun createSplitRowWithGap(
        row: List<KeyDef>,
        gapRatio: Float,
        rowScale: Float
    ): ConstraintLayout {
        val (leftRow, rightRow) = splitRow(row)
        val leftRaw = rowPercent(leftRow)
        val rightRaw = rowPercent(rightRow)
        val totalRaw = (leftRaw + rightRaw).coerceAtLeast(1e-6f)
        val scale = (rowScale / totalRaw).coerceAtLeast(0f)
        val leftWidth = leftRaw * scale
        val rightWidth = rightRaw * scale
        val groupTotal = (leftWidth + rightWidth + gapRatio).coerceAtMost(1f)
        val leftInGroup = if (groupTotal > 0f) leftWidth / groupTotal else 0f
        val gapInGroup = if (groupTotal > 0f) gapRatio / groupTotal else 0f
        val rightInGroup = if (groupTotal > 0f) rightWidth / groupTotal else 0f
        val leftScale = if (leftRaw > 0f) 1f / leftRaw else 1f
        val rightScale = if (rightRaw > 0f) 1f / rightRaw else 1f
        return constraintLayout {
            val group = constraintLayout {
                val gap = view(::View)
                val leftLayout = createKeyRow(
                    leftRow,
                    chainBias = 1f,
                    widthScale = leftScale,
                    allowExpand = false
                )
                val rightLayout = createKeyRow(
                    rightRow,
                    chainBias = 0f,
                    widthScale = rightScale,
                    allowExpand = false
                )
                add(leftLayout, lParams(0, matchParent) {
                    startOfParent()
                    topOfParent()
                    bottomOfParent()
                    matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                    matchConstraintPercentWidth = leftInGroup
                })
                add(gap, lParams(0, matchParent) {
                    startToEndOf(leftLayout)
                    topOfParent()
                    bottomOfParent()
                    matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                    matchConstraintPercentWidth = gapInGroup
                })
                add(rightLayout, lParams(0, matchParent) {
                    startToEndOf(gap)
                    endOfParent()
                    topOfParent()
                    bottomOfParent()
                    matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                    matchConstraintPercentWidth = rightInGroup
                })
            }
            add(group, lParams(0, matchParent) {
                startOfParent()
                endOfParent()
                topOfParent()
                bottomOfParent()
                matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                matchConstraintPercentWidth = groupTotal
            })
        }
    }

    private fun createSpaceSplitRow(
        row: List<KeyDef>,
        gapRatio: Float
    ): ConstraintLayout {
        val spaceIndex = row.indexOfFirst { it is SpaceKey || it is MiniSpaceKey }
        if (spaceIndex < 0) return createKeyRow(row)
        val leftRow = row.subList(0, spaceIndex)
        val spaceDef = row[spaceIndex]
        val rightRow = row.subList(spaceIndex + 1, row.size)
        val leftRaw = rowPercent(leftRow)
        val rightRaw = rowPercent(rightRow)
        val ratio = (1f - gapRatio).coerceAtLeast(0f)
        val leftWidth = (leftRaw * ratio).coerceAtLeast(0f)
        val rightWidth = (rightRaw * ratio).coerceAtLeast(0f)
        val spaceWidth = (1f - leftWidth - rightWidth).coerceAtLeast(0f)
        val leftScale = if (leftRaw > 0f) 1f / leftRaw else 1f
        val rightScale = if (rightRaw > 0f) 1f / rightRaw else 1f
        return constraintLayout {
            val group = constraintLayout {
                val leftLayout = createKeyRow(
                    leftRow,
                    chainBias = 1f,
                    widthScale = leftScale,
                    allowExpand = false
                )
                val rightLayout = createKeyRow(
                    rightRow,
                    chainBias = 0f,
                    widthScale = rightScale,
                    allowExpand = false
                )
                val spaceView = createKeyView(spaceDef)
                add(leftLayout, lParams(0, matchParent) {
                    startOfParent()
                    topOfParent()
                    bottomOfParent()
                    matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                    matchConstraintPercentWidth = leftWidth
                })
                add(spaceView, lParams(0, matchParent) {
                    startToEndOf(leftLayout)
                    topOfParent()
                    bottomOfParent()
                    matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                    matchConstraintPercentWidth = spaceWidth
                })
                add(rightLayout, lParams(0, matchParent) {
                    startToEndOf(spaceView)
                    endOfParent()
                    topOfParent()
                    bottomOfParent()
                    matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                    matchConstraintPercentWidth = rightWidth
                })
            }
            add(group, lParams(0, matchParent) {
                startOfParent()
                endOfParent()
                topOfParent()
                bottomOfParent()
                matchConstraintDefaultWidth = LayoutParams.MATCH_CONSTRAINT_PERCENT
                matchConstraintPercentWidth = 1f
            })
        }
    }

    private fun rowPercent(row: List<KeyDef>): Float {
        var total = 0f
        row.forEach { def ->
            if (def is SpaceKey || def is MiniSpaceKey) return@forEach
            val width = def.appearance.percentWidth
            if (width > 0f) {
                total += width
            }
        }
        return total
    }

    private fun isStandardWidthRow(row: List<KeyDef>): Boolean {
        val standard = 0.1f
        val eps = 1e-4f
        return row.all { def ->
            if (def is SpaceKey || def is MiniSpaceKey) true
            else (def.appearance.percentWidth - standard).absoluteValue <= eps
        }
    }

    private fun splitRow(row: List<KeyDef>): Pair<List<KeyDef>, List<KeyDef>> {
        if (row.isEmpty()) return emptyList<KeyDef>() to emptyList()
        val mid = row.size / 2
        return if (row.size % 2 == 0) {
            row.subList(0, mid) to row.subList(mid, row.size)
        } else {
            row.subList(0, mid + 1) to row.subList(mid, row.size)
        }
    }

    private fun createKeyRow(
        row: List<KeyDef>,
        chainBias: Float? = null,
        widthScale: Float = 1f,
        allowExpand: Boolean = true
    ): ConstraintLayout {
        val keyViews = row.map(::createKeyView)
        if (keyViews.isEmpty()) {
            return constraintLayout { }
        }
        return constraintLayout Row@{
            var totalWidth = 0f
            keyViews.forEachIndexed { index, view ->
                add(view, lParams {
                    centerVertically()
                    if (index == 0) {
                        leftOfParent()
                        horizontalChainStyle = LayoutParams.CHAIN_PACKED
                        if (chainBias != null) {
                            horizontalBias = chainBias
                        }
                    } else {
                        leftToRightOf(keyViews[index - 1])
                    }
                    if (index == keyViews.size - 1) {
                        rightOfParent()
                        // for RTL
                        horizontalChainStyle = LayoutParams.CHAIN_PACKED
                    } else {
                        rightToLeftOf(keyViews[index + 1])
                    }
                    val def = row[index]
                    val baseWidth = def.appearance.percentWidth
                    matchConstraintPercentWidth = if (baseWidth == 0f) 0f else baseWidth * widthScale
                })
                val baseWidth = row[index].appearance.percentWidth
                val scaledWidth = if (baseWidth == 0f) 0f else baseWidth * widthScale
                scaledWidth.let {
                    // 0f means fill remaining space, thus does not need expanding
                    totalWidth += if (it != 0f) it else 1f
                }
            }
            if (allowExpand && expandKeypressArea && totalWidth < 1f) {
                val free = (1f - totalWidth) / 2f
                val firstBase = row.first().appearance.percentWidth
                val lastBase = row.last().appearance.percentWidth
                val firstScaled = if (firstBase == 0f) 0f else firstBase * widthScale
                val lastScaled = if (lastBase == 0f) 0f else lastBase * widthScale
                keyViews.first().apply {
                    updateLayoutParams<LayoutParams> {
                        matchConstraintPercentWidth += free
                    }
                    layoutMarginLeft = free / (firstScaled + free)
                }
                keyViews.last().apply {
                    updateLayoutParams<LayoutParams> {
                        matchConstraintPercentWidth += free
                    }
                    layoutMarginRight = free / (lastScaled + free)
                }
            }
        }
    }

    private fun createKeyView(def: KeyDef): KeyView {
        return when (def.appearance) {
            is KeyDef.Appearance.AltText -> AltTextKeyView(context, theme, def.appearance)
            is KeyDef.Appearance.ImageText -> ImageTextKeyView(context, theme, def.appearance)
            is KeyDef.Appearance.Text -> TextKeyView(context, theme, def.appearance)
            is KeyDef.Appearance.Image -> ImageKeyView(context, theme, def.appearance)
        }.apply {
            soundEffect = when (def) {
                is SpaceKey -> InputFeedbacks.SoundEffect.SpaceBar
                is MiniSpaceKey -> InputFeedbacks.SoundEffect.SpaceBar
                is BackspaceKey -> InputFeedbacks.SoundEffect.Delete
                is ReturnKey -> InputFeedbacks.SoundEffect.Return
                else -> InputFeedbacks.SoundEffect.Standard
            }
            if (def is SpaceKey) {
                spaceKeys.add(this)
                swipeEnabled = spaceSwipeMoveCursor.getValue()
                swipeRepeatEnabled = true
                swipeThresholdX = selectionSwipeThreshold
                swipeThresholdY = disabledSwipeThreshold
                onGestureListener = OnGestureListener { view, event ->
                    when (event.type) {
                        GestureType.Move -> when (val count = event.countX) {
                            0 -> false
                            else -> {
                                val sym =
                                    if (count > 0) FcitxKeyMapping.FcitxKey_Right else FcitxKeyMapping.FcitxKey_Left
                                val action = KeyAction.SymAction(KeySym(sym), KeyStates.Virtual)
                                repeat(count.absoluteValue) {
                                    onAction(action)
                                    if (hapticOnRepeat) InputFeedbacks.hapticFeedback(view)
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
                swipeThresholdY = disabledSwipeThreshold
                onGestureListener = OnGestureListener { view, event ->
                    when (event.type) {
                        GestureType.Move -> {
                            val count = event.countX
                            if (count != 0) {
                                onAction(KeyAction.MoveSelectionAction(count))
                                if (hapticOnRepeat) InputFeedbacks.hapticFeedback(view)
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
                        onRepeatListener = { view ->
                            onAction(it.action)
                            if (hapticOnRepeat) InputFeedbacks.hapticFeedback(view)
                        }
                    }
                    is KeyDef.Behavior.Swipe -> {
                        swipeEnabled = true
                        swipeThresholdX = disabledSwipeThreshold
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
                            onPopupAction(PopupAction.ShowMenuAction(view.id, it, view.bounds))
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
                            onPopupAction(PopupAction.ShowKeyboardAction(view.id, it, view.bounds))
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
                            if (popupOnKeyPress) {
                                when (event.type) {
                                    GestureType.Down -> onPopupAction(
                                        PopupAction.PreviewAction(view.id, it.content, view.bounds)
                                    )
                                    GestureType.Move -> {
                                        val triggered = swipeSymbolDirection.checkY(event.totalY)
                                        val text = if (triggered) it.alternative else it.content
                                        onPopupAction(
                                            PopupAction.PreviewUpdateAction(view.id, text)
                                        )
                                    }
                                    GestureType.Up -> {
                                        onPopupAction(PopupAction.DismissAction(view.id))
                                    }
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
                            if (popupOnKeyPress) {
                                when (event.type) {
                                    GestureType.Down -> onPopupAction(
                                        PopupAction.PreviewAction(view.id, it.content, view.bounds)
                                    )
                                    GestureType.Up -> {
                                        onPopupAction(PopupAction.DismissAction(view.id))
                                    }
                                    else -> {}
                                }
                            }
                            // never consume gesture in preview popup
                            oldOnGestureListener.onGesture(view, event)
                        }
                    }
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            lastMeasuredWidth = w
            lastMeasuredHeight = h
        }
        val allowed = isSplitAllowed(w, h)
        if (allowed != lastSplitAllowed) {
            lastSplitAllowed = allowed
            rebuildKeyboardRows(splitKeyboard.getValue())
        }
        val (x, y) = intArrayOf(0, 0).also { getLocationInWindow(it) }
        bounds.set(x, y, x + width, y + height)
    }

    private fun findTargetChild(x: Float, y: Float): View? {
        val y0 = y.roundToInt()
        // assume all rows have equal height
        val row = keyRows.getOrNull(y0 * keyRows.size / bounds.height()) ?: return null
        val x1 = x.roundToInt() + bounds.left
        val y1 = y0 + bounds.top
        return row.allViews.firstOrNull {
            it is KeyView && it.bounds.contains(x1, y1)
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
    protected open fun onAction(
        action: KeyAction,
        source: KeyActionListener.Source = KeyActionListener.Source.Keyboard
    ) {
        keyActionListener?.onKeyAction(action, source)
    }

    @CallSuper
    protected open fun onPopupAction(action: PopupAction) {
        popupActionListener?.onPopupAction(action)
    }

    private fun onPopupChangeFocus(viewId: Int, x: Float, y: Float): Boolean {
        val changeFocusAction = PopupAction.ChangeFocusAction(viewId, x, y)
        popupActionListener?.onPopupAction(changeFocusAction)
        return changeFocusAction.outResult
    }

    private fun onPopupTrigger(viewId: Int): Boolean {
        val triggerAction = PopupAction.TriggerAction(viewId)
        // ask popup keyboard whether there's a pending KeyAction
        onPopupAction(triggerAction)
        val action = triggerAction.outAction ?: return false
        onAction(action, KeyActionListener.Source.Popup)
        onPopupAction(PopupAction.DismissAction(viewId))
        return true
    }

    open fun onAttach() {
        layoutCallbacksEnabled = true
        onKeyboardLayoutRebuilt()
    }

    open fun onReturnDrawableUpdate(@DrawableRes returnDrawable: Int) {
        // do nothing by default
    }

    open fun onPunctuationUpdate(mapping: Map<String, String>) {
        // do nothing by default
    }

    @CallSuper
    open fun onInputMethodUpdate(ime: InputMethodEntry) {
        lastInputMethod = ime
    }

    protected open fun onKeyboardLayoutRebuilt() {
        // for subclasses
    }

    fun refreshLayoutForPrefs() {
        rebuildKeyboardRows(splitKeyboard.getValue())
    }

    open fun onDetach() {
        layoutCallbacksEnabled = false
    }

}
