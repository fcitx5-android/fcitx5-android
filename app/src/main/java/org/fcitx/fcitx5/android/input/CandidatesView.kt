/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.WindowInsets
import android.widget.TextView
import androidx.annotation.Size
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.floating.PagedCandidatesUi
import org.fcitx.fcitx5.android.input.preedit.PreeditUi
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.padding

@SuppressLint("ViewConstructor")
class CandidatesView(
    service: FcitxInputMethodService,
    fcitx: FcitxConnection,
    theme: Theme
) : BaseInputView(service, fcitx, theme) {

    private val ctx = context.withTheme(R.style.Theme_InputViewTheme)

    private val candidatesPrefs = AppPrefs.getInstance().candidates
    private val orientation by candidatesPrefs.orientation
    private val windowMinWidth by candidatesPrefs.windowMinWidth
    private val windowPadding by candidatesPrefs.windowPadding
    private val fontSize by candidatesPrefs.fontSize
    private val itemPaddingVertical by candidatesPrefs.itemPaddingVertical
    private val itemPaddingHorizontal by candidatesPrefs.itemPaddingHorizontal

    private var inputPanel = FcitxEvent.InputPanelEvent.Data()
    private var paged = FcitxEvent.PagedCandidateEvent.Data.Empty

    /**
     * horizontal, bottom, top
     */
    private val anchorPosition = floatArrayOf(0f, 0f, 0f)
    private val parentSize = floatArrayOf(0f, 0f)

    private var shouldUpdatePosition = false

    private val layoutListener = OnGlobalLayoutListener {
        shouldUpdatePosition = true
    }

    private val preDrawListener = OnPreDrawListener {
        if (shouldUpdatePosition) {
            updatePosition()
        }
        true
    }

    private val setupTextView: TextView.() -> Unit = {
        textSize = fontSize.toFloat()
        val v = dp(itemPaddingVertical)
        val h = dp(itemPaddingHorizontal)
        setPadding(h, v, h, v)
    }

    private val preeditUi = PreeditUi(ctx, theme, setupTextView)

    private val candidatesUi = PagedCandidatesUi(ctx, theme, setupTextView).apply {
        root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private var bottomInsets = 0

    override fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.InputPanelEvent -> {
                inputPanel = it.data
                updateUi()
            }
            is FcitxEvent.PagedCandidateEvent -> {
                paged = it.data
                updateUi()
            }
            else -> {}
        }
    }

    private fun evaluateVisibility(): Boolean {
        return inputPanel.preedit.isNotEmpty() ||
                paged.candidates.isNotEmpty() ||
                inputPanel.auxUp.isNotEmpty() ||
                inputPanel.auxDown.isNotEmpty()
    }

    private fun updateUi() {
        if (evaluateVisibility()) {
            preeditUi.update(inputPanel)
            preeditUi.root.visibility = if (preeditUi.visible) VISIBLE else GONE
            candidatesUi.update(paged, orientation)
            visibility = VISIBLE
        } else {
            visibility = GONE
        }
    }

    private fun updatePosition() {
        val (horizontal, bottom, top) = anchorPosition
        val (parentWidth, parentHeight) = parentSize
        val selfWidth = width.toFloat()
        val selfHeight = height.toFloat()
        if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            val rtlOffset = parentWidth - horizontal
            translationX =
                if (rtlOffset + selfWidth > parentWidth) selfWidth - parentWidth else -rtlOffset
        } else {
            translationX =
                if (horizontal + selfWidth > parentWidth) parentWidth - selfWidth else horizontal
        }
        translationY =
            if (bottom + selfHeight > parentHeight - bottomInsets) top - selfHeight else bottom
        shouldUpdatePosition = false
    }

    fun updateCursorAnchor(@Size(4) anchor: FloatArray, @Size(2) parent: FloatArray) {
        val (horizontal, bottom, _, top) = anchor
        val (parentWidth, parentHeight) = parent
        anchorPosition[0] = horizontal
        anchorPosition[1] = bottom
        anchorPosition[2] = top
        parentSize[0] = parentWidth
        parentSize[1] = parentHeight
        updatePosition()
    }

    init {
        // invisible by default
        visibility = GONE

        minWidth = dp(windowMinWidth)
        padding = dp(windowPadding)
        backgroundColor = theme.backgroundColor
        add(preeditUi.root, lParams(wrapContent, wrapContent) {
            topOfParent()
            startOfParent()
        })
        add(candidatesUi.root, lParams(matchConstraints, wrapContent) {
            matchConstraintMinWidth = wrapContent
            below(preeditUi.root)
            centerHorizontally()
            bottomOfParent()
        })

        isFocusable = false
        layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            bottomInsets = getNavBarBottomInset(insets)
        }
        return insets
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        candidatesUi.root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        candidatesUi.root.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        super.onDetachedFromWindow()
    }
}
