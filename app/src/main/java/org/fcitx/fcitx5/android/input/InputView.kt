/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsResponse
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.broadcast.PreeditEmptyStateComponent
import org.fcitx.fcitx5.android.input.broadcast.PunctuationComponent
import org.fcitx.fcitx5.android.input.broadcast.ReturnKeyDrawableComponent
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.picker.emojiPicker
import org.fcitx.fcitx5.android.input.picker.emoticonPicker
import org.fcitx.fcitx5.android.input.picker.symbolPicker
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.unset
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.wrapToUniqueComponent
import org.mechdancer.dependency.plusAssign
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.endToStartOf
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.startToEndOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable
import splitties.views.imageResource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class InputView(
    service: FcitxInputMethodService,
    fcitx: FcitxConnection,
    theme: Theme
) : BaseInputView(service, fcitx, theme) {

    companion object {
        private const val MIN_FLOATING_KEYBOARD_WIDTH_PERCENT = 55
        private const val MAX_FLOATING_KEYBOARD_WIDTH_PERCENT = 100
        private const val FLOATING_DRAG_HANDLE_HEIGHT_DP = 24
        private const val FLOATING_RESIZE_HANDLE_SIZE_DP = 40
    }

    private val keyBorder by ThemeManager.prefs.keyBorder

    private val customBackground = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val placeholderOnClickListener = OnClickListener { }

    // use clickable view as padding, so MotionEvent can be split to padding view and keyboard view
    private val leftPaddingSpace = view(::View) {
        setOnClickListener(placeholderOnClickListener)
    }
    private val rightPaddingSpace = view(::View) {
        setOnClickListener(placeholderOnClickListener)
    }
    private val bottomPaddingSpace = view(::View) {
        // height as keyboardBottomPadding
        // bottomMargin as WindowInsets (Navigation Bar) offset
        setOnClickListener(placeholderOnClickListener)
    }
    private val resizeHandle = imageView {
        imageResource = R.drawable.ic_baseline_drag_handle_24
        scaleType = ImageView.ScaleType.CENTER
        alpha = 0.72f
        contentDescription = context.getString(R.string.resize_floating_keyboard)
    }

    private val scope = DynamicScope()
    private val themedContext = context.withTheme(R.style.Theme_InputViewTheme)
    private val broadcaster = InputBroadcaster()
    private val popup = PopupComponent()
    private val punctuation = PunctuationComponent()
    private val returnKeyDrawable = ReturnKeyDrawableComponent()
    private val preeditEmptyState = PreeditEmptyStateComponent()
    private val preedit = PreeditComponent()
    private val commonKeyActionListener = CommonKeyActionListener()
    private val windowManager = InputWindowManager()
    private val kawaiiBar = KawaiiBarComponent()
    private val horizontalCandidate = HorizontalCandidateComponent()
    private val keyboardWindow = KeyboardWindow()
    private val symbolPicker = symbolPicker()
    private val emojiPicker = emojiPicker()
    private val emoticonPicker = emoticonPicker()

    private fun setupScope() {
        scope += this@InputView.wrapToUniqueComponent()
        scope += service.wrapToUniqueComponent()
        scope += fcitx.wrapToUniqueComponent()
        scope += theme.wrapToUniqueComponent()
        scope += themedContext.wrapToUniqueComponent()
        scope += broadcaster
        scope += popup
        scope += punctuation
        scope += returnKeyDrawable
        scope += preeditEmptyState
        scope += preedit
        scope += commonKeyActionListener
        scope += windowManager
        scope += kawaiiBar
        scope += horizontalCandidate
        broadcaster.onScopeSetupFinished(scope)
    }

    private val keyboardPrefs = AppPrefs.getInstance().keyboard
    private val internalPrefs = AppPrefs.getInstance().internal

    private val focusChangeResetKeyboard by keyboardPrefs.focusChangeResetKeyboard

    private var floatingKeyboardEnabled by keyboardPrefs.floatingKeyboardEnabled
    private val keyboardHeightPercent = keyboardPrefs.keyboardHeightPercent
    private val keyboardHeightPercentLandscape = keyboardPrefs.keyboardHeightPercentLandscape
    private val keyboardSidePadding = keyboardPrefs.keyboardSidePadding
    private val keyboardSidePaddingLandscape = keyboardPrefs.keyboardSidePaddingLandscape
    private val keyboardBottomPadding = keyboardPrefs.keyboardBottomPadding
    private val keyboardBottomPaddingLandscape = keyboardPrefs.keyboardBottomPaddingLandscape
    private val floatingKeyboardWidthPercent = keyboardPrefs.floatingKeyboardWidthPercent
    private val floatingKeyboardWidthPercentLandscape = keyboardPrefs.floatingKeyboardWidthPercentLandscape

    private val keyboardSizePrefs = listOf(
        keyboardPrefs.floatingKeyboardEnabled,
        keyboardHeightPercent,
        keyboardHeightPercentLandscape,
        keyboardSidePadding,
        keyboardSidePaddingLandscape,
        keyboardBottomPadding,
        keyboardBottomPaddingLandscape,
        floatingKeyboardWidthPercent,
        floatingKeyboardWidthPercentLandscape,
    )

    private val keyboardHeightPx: Int
        get() {
            val percent = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardHeightPercentLandscape
                else -> keyboardHeightPercent
            }.getValue()
            return (if (floatingKeyboardEnabled) {
                height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
            } else {
                resources.displayMetrics.heightPixels
            }) * percent / 100
        }

    private val keyboardSidePaddingPx: Int
        get() {
            val value = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardSidePaddingLandscape
                else -> keyboardSidePadding
            }.getValue()
            return dp(value)
        }

    private val keyboardBottomPaddingPx: Int
        get() {
            val value = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardBottomPaddingLandscape
                else -> keyboardBottomPadding
            }.getValue()
            return dp(value)
        }

    private val floatingKeyboardWidthPx: Int
        get() {
            val parentWidth = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
            return parentWidth * activeFloatingKeyboardWidthPercent.getValue() / 100
        }

    private val floatingDragHandleHeightPx: Int
        get() = dp(FLOATING_DRAG_HANDLE_HEIGHT_DP)

    private val activeFloatingKeyboardWidthPercent
        get() = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> floatingKeyboardWidthPercentLandscape
            else -> floatingKeyboardWidthPercent
        }

    private val activeFloatingKeyboardXRatio
        get() = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> internalPrefs.floatingKeyboardXRatioLandscape
            else -> internalPrefs.floatingKeyboardXRatio
        }

    private val activeFloatingKeyboardYRatio
        get() = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> internalPrefs.floatingKeyboardYRatioLandscape
            else -> internalPrefs.floatingKeyboardYRatio
        }

    private var navBarBottomInset = 0
    private var floatingKeyboardX = 0f
    private var floatingKeyboardY = 0f
    private var floatingDragStartRawX = 0f
    private var floatingDragStartRawY = 0f
    private var floatingDragStartKeyboardX = 0f
    private var floatingDragStartKeyboardY = 0f
    private var floatingResizeStartWidth = 0
    private val inputViewLocation = intArrayOf(0, 0)

    @Keep
    private val onKeyboardSizeChangeListener = ManagedPreferenceProvider.OnChangeListener { key ->
        if (keyboardPrefs.floatingKeyboardEnabled.key == key) {
            applyKeyboardMode()
        } else if (keyboardSizePrefs.any { it.key == key }) {
            updateKeyboardSize()
        }
    }

    val keyboardView: View

    private val floatingKeyboardDragListener = OnTouchListener { _, event ->
        if (!floatingKeyboardEnabled) return@OnTouchListener false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                floatingDragStartRawX = event.rawX
                floatingDragStartRawY = event.rawY
                floatingDragStartKeyboardX = floatingKeyboardX
                floatingDragStartKeyboardY = floatingKeyboardY
                true
            }

            MotionEvent.ACTION_MOVE -> {
                updateFloatingKeyboardPosition(
                    floatingDragStartKeyboardX + event.rawX - floatingDragStartRawX,
                    floatingDragStartKeyboardY + event.rawY - floatingDragStartRawY,
                    persist = true
                )
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
            else -> false
        }
    }

    private val floatingKeyboardResizeListener = OnTouchListener { _, event ->
        if (!floatingKeyboardEnabled) return@OnTouchListener false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                floatingDragStartRawX = event.rawX
                floatingResizeStartWidth = keyboardView.width
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val parentWidth = width.takeIf { it > 0 } ?: return@OnTouchListener true
                val delta = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                    floatingDragStartRawX - event.rawX
                } else {
                    event.rawX - floatingDragStartRawX
                }
                val newWidth = (floatingResizeStartWidth + delta).roundToInt()
                val percent = (newWidth * 100f / parentWidth)
                    .roundToInt()
                    .coerceIn(MIN_FLOATING_KEYBOARD_WIDTH_PERCENT, MAX_FLOATING_KEYBOARD_WIDTH_PERCENT)
                activeFloatingKeyboardWidthPercent.setValue(percent)
                updateKeyboardSize()
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
            else -> false
        }
    }

    init {
        // MUST call before any operation
        setupScope()

        // restore punctuation mapping in case of InputView recreation
        fcitx.launchOnReady {
            punctuation.updatePunctuationMapping(it.statusAreaActionsCached)
        }

        // make sure KeyboardWindow's view has been created before it receives any broadcast
        windowManager.addEssentialWindow(keyboardWindow, createView = true)
        windowManager.addEssentialWindow(symbolPicker)
        windowManager.addEssentialWindow(emojiPicker)
        windowManager.addEssentialWindow(emoticonPicker)
        // show KeyboardWindow by default
        windowManager.attachWindow(KeyboardWindow)

        broadcaster.onImeUpdate(fcitx.runImmediately { inputMethodEntryCached })

        customBackground.imageDrawable = theme.backgroundDrawable(keyBorder)

        keyboardView = constraintLayout {
            // allow MotionEvent to be delivered to keyboard while pressing on padding views.
            // although it should be default for apps targeting Honeycomb (3.0, API 11) and higher,
            // but it's not the case on some devices ... just set it here
            isMotionEventSplittingEnabled = true
            add(customBackground, lParams {
                centerVertically()
                centerHorizontally()
            })
            add(kawaiiBar.view, lParams(matchParent, dp(KawaiiBarComponent.HEIGHT)) {
                topOfParent()
                centerHorizontally()
            })
            add(leftPaddingSpace, lParams {
                below(kawaiiBar.view)
                startOfParent()
                bottomOfParent()
            })
            add(rightPaddingSpace, lParams {
                below(kawaiiBar.view)
                endOfParent()
                bottomOfParent()
            })
            add(windowManager.view, lParams {
                below(kawaiiBar.view)
                above(bottomPaddingSpace)
                /**
                 * set start and end constrain in [updateKeyboardSize]
                 */
            })
            add(bottomPaddingSpace, lParams {
                startToEndOf(leftPaddingSpace)
                endToStartOf(rightPaddingSpace)
                bottomOfParent()
            })
            add(resizeHandle, lParams(dp(FLOATING_RESIZE_HANDLE_SIZE_DP), dp(FLOATING_RESIZE_HANDLE_SIZE_DP)) {
                endOfParent()
                bottomOfParent()
            })
        }

        bottomPaddingSpace.setOnTouchListener(floatingKeyboardDragListener)
        resizeHandle.setOnTouchListener(floatingKeyboardResizeListener)

        updateKeyboardSize()

        add(preedit.ui.root, lParams(matchParent, wrapContent) {
            above(keyboardView)
            centerHorizontally()
        })
        if (floatingKeyboardEnabled) {
            add(keyboardView, lParams(floatingKeyboardWidthPx, wrapContent) {
                startOfParent()
                topOfParent()
            })
        } else {
            add(keyboardView, lParams(matchParent, wrapContent) {
                centerHorizontally()
                bottomOfParent()
            })
        }
        post { applyKeyboardMode() }
        add(popup.root, lParams(matchParent, matchParent) {
            centerVertically()
            centerHorizontally()
        })

        keyboardPrefs.registerOnChangeListener(onKeyboardSizeChangeListener)
    }

    private fun updateKeyboardSize() {
        windowManager.view.updateLayoutParams {
            height = keyboardHeightPx
        }
        bottomPaddingSpace.updateLayoutParams {
            height = if (floatingKeyboardEnabled) {
                max(keyboardBottomPaddingPx, floatingDragHandleHeightPx)
            } else {
                keyboardBottomPaddingPx
            }
        }
        val sidePadding = keyboardSidePaddingPx
        if (floatingKeyboardEnabled) {
            leftPaddingSpace.visibility = GONE
            rightPaddingSpace.visibility = GONE
            resizeHandle.visibility = VISIBLE
            windowManager.view.updateLayoutParams<LayoutParams> {
                startToEnd = unset
                endToStart = unset
                startOfParent()
                endOfParent()
            }
            bottomPaddingSpace.updateLayoutParams<LayoutParams> {
                startToEnd = unset
                endToStart = unset
                startOfParent()
                endOfParent()
            }
        } else if (sidePadding == 0) {
            // hide side padding space views when unnecessary
            leftPaddingSpace.visibility = GONE
            rightPaddingSpace.visibility = GONE
            resizeHandle.visibility = GONE
            windowManager.view.updateLayoutParams<LayoutParams> {
                startToEnd = unset
                endToStart = unset
                startOfParent()
                endOfParent()
            }
        } else {
            leftPaddingSpace.visibility = VISIBLE
            rightPaddingSpace.visibility = VISIBLE
            resizeHandle.visibility = GONE
            leftPaddingSpace.updateLayoutParams {
                width = sidePadding
            }
            rightPaddingSpace.updateLayoutParams {
                width = sidePadding
            }
            windowManager.view.updateLayoutParams<LayoutParams> {
                startToStart = unset
                endToEnd = unset
                startToEndOf(leftPaddingSpace)
                endToStartOf(rightPaddingSpace)
            }
        }
        val inputSidePadding = if (floatingKeyboardEnabled) 0 else sidePadding
        preedit.ui.root.setPadding(inputSidePadding, 0, inputSidePadding, 0)
        kawaiiBar.view.setPadding(inputSidePadding, 0, inputSidePadding, 0)
        if (floatingKeyboardEnabled) {
            if (keyboardView.parent != null) {
                keyboardView.updateLayoutParams<LayoutParams> {
                    width = floatingKeyboardWidthPx
                }
            }
            keyboardView.post {
                restoreFloatingKeyboardPosition()
            }
        }
        service.window.window?.decorView?.requestLayout()
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        navBarBottomInset = getNavBarBottomInset(insets)
        bottomPaddingSpace.updateLayoutParams<LayoutParams> {
            bottomMargin = navBarBottomInset
        }
        if (floatingKeyboardEnabled) {
            keyboardView.post {
                restoreFloatingKeyboardPosition()
            }
        }
        return insets
    }

    private fun applyKeyboardMode() {
        keyboardView.updateLayoutParams<LayoutParams> {
            if (floatingKeyboardEnabled) {
                width = floatingKeyboardWidthPx
                height = wrapContent
                startToStart = PARENT_ID
                topToTop = PARENT_ID
                endToEnd = unset
                bottomToBottom = unset
            } else {
                width = matchParent
                height = wrapContent
                startToStart = unset
                topToTop = unset
                endToEnd = unset
                bottomToBottom = PARENT_ID
                centerHorizontally()
            }
        }
        keyboardView.translationX = 0f
        keyboardView.translationY = 0f
        updateKeyboardSize()
        kawaiiBar.updateFloatingKeyboardButton()
        if (floatingKeyboardEnabled) {
            keyboardView.post { restoreFloatingKeyboardPosition() }
        }
    }

    private fun restoreFloatingKeyboardPosition() {
        if (!floatingKeyboardEnabled || width <= 0 || keyboardView.width <= 0) return
        val maxX = max(0, width - keyboardView.width).toFloat()
        val maxY = max(0, height - navBarBottomInset - keyboardView.height).toFloat()
        val x = maxX * activeFloatingKeyboardXRatio.getValue().coerceIn(0f, 1f)
        val y = maxY * activeFloatingKeyboardYRatio.getValue().coerceIn(0f, 1f)
        updateFloatingKeyboardPosition(x, y)
    }

    private fun updateFloatingKeyboardPosition(x: Float, y: Float, persist: Boolean = false) {
        if (!floatingKeyboardEnabled) return
        val maxX = max(0, width - keyboardView.width).toFloat()
        val maxY = max(0, height - navBarBottomInset - keyboardView.height).toFloat()
        floatingKeyboardX = min(max(x, 0f), maxX)
        floatingKeyboardY = min(max(y, 0f), maxY)
        keyboardView.translationX = floatingKeyboardX
        keyboardView.translationY = floatingKeyboardY
        if (persist) {
            activeFloatingKeyboardXRatio.setValue(if (maxX > 0f) floatingKeyboardX / maxX else 0.5f)
            activeFloatingKeyboardYRatio.setValue(if (maxY > 0f) floatingKeyboardY / maxY else 1f)
        }
        service.window.window?.decorView?.requestLayout()
    }

    fun getFloatingKeyboardTouchableRect(outRect: Rect): Boolean {
        if (!floatingKeyboardEnabled || keyboardView.width <= 0 || keyboardView.height <= 0) {
            return false
        }
        keyboardView.getLocationInWindow(inputViewLocation)
        outRect.set(
            inputViewLocation[0],
            inputViewLocation[1],
            inputViewLocation[0] + keyboardView.width,
            inputViewLocation[1] + keyboardView.height
        )
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (floatingKeyboardEnabled) {
            keyboardView.post {
                restoreFloatingKeyboardPosition()
            }
        }
    }

    fun toggleFloatingKeyboard() {
        floatingKeyboardEnabled = !floatingKeyboardEnabled
        applyKeyboardMode()
    }

    fun isFloatingKeyboardEnabled() = floatingKeyboardEnabled

    /**
     * called when [InputView] is about to show, or restart
     */
    fun startInput(info: EditorInfo, capFlags: CapabilityFlags, restarting: Boolean = false) {
        broadcaster.onStartInput(info, capFlags)
        returnKeyDrawable.updateDrawableOnEditorInfo(info)
        if (focusChangeResetKeyboard || !restarting) {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

    override fun onStartHandleFcitxEvent() {
        val inputPanelData = fcitx.runImmediately { inputPanelCached }
        val inputMethodEntry = fcitx.runImmediately { inputMethodEntryCached }
        val statusAreaActions = fcitx.runImmediately { statusAreaActionsCached }
        arrayOf(
            FcitxEvent.InputPanelEvent(inputPanelData),
            FcitxEvent.IMChangeEvent(inputMethodEntry),
            FcitxEvent.StatusAreaEvent(
                FcitxEvent.StatusAreaEvent.Data(statusAreaActions, inputMethodEntry)
            )
        ).forEach { handleFcitxEvent(it) }
    }

    override fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                broadcaster.onCandidateUpdate(it.data)
            }
            is FcitxEvent.ClientPreeditEvent -> {
                preeditEmptyState.updatePreeditEmptyState(clientPreedit = it.data)
                broadcaster.onClientPreeditUpdate(it.data)
            }
            is FcitxEvent.InputPanelEvent -> {
                preeditEmptyState.updatePreeditEmptyState(preedit = it.data.preedit)
                broadcaster.onInputPanelUpdate(it.data)
            }
            is FcitxEvent.IMChangeEvent -> {
                broadcaster.onImeUpdate(it.data)
            }
            is FcitxEvent.StatusAreaEvent -> {
                punctuation.updatePunctuationMapping(it.data.actions)
                broadcaster.onStatusAreaUpdate(it.data.actions)
            }
            else -> {}
        }
    }

    fun updateSelection(start: Int, end: Int) {
        broadcaster.onSelectionUpdate(start, end)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun handleInlineSuggestions(response: InlineSuggestionsResponse): Boolean {
        return kawaiiBar.handleInlineSuggestions(response)
    }

    override fun onDetachedFromWindow() {
        keyboardPrefs.unregisterOnChangeListener(onKeyboardSizeChangeListener)
        // clear DynamicScope, implies that InputView should not be attached again after detached.
        scope.clear()
        super.onDetachedFromWindow()
    }

}
