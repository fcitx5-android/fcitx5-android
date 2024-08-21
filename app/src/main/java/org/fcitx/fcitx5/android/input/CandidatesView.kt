/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Size
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.CandidateItemUi
import org.fcitx.fcitx5.android.input.preedit.PreeditUi
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.horizontalPadding
import splitties.views.verticalPadding

@SuppressLint("ViewConstructor")
class CandidatesView(
    val service: FcitxInputMethodService,
    val fcitx: FcitxConnection,
    val theme: Theme
) : ConstraintLayout(service) {

    var handleEvents = false
        set(value) {
            field = value
            visibility = View.GONE
            if (field) {
                setupFcitxEventHandler()
            } else {
                eventHandlerJob?.cancel()
                eventHandlerJob = null
            }
        }

    private val ctx = context.withTheme(R.style.Theme_InputViewTheme)

    private var eventHandlerJob: Job? = null

    private var inputPanel = FcitxEvent.InputPanelEvent.Data()
    private var paged = FcitxEvent.PagedCandidateEvent.Data.Empty

    /**
     * horizontal, bottom, top
     */
    private val anchorPosition = floatArrayOf(0f, 0f, 0f)
    private val parentSize = floatArrayOf(0f, 0f)

    private val preeditUi = object : PreeditUi(ctx, theme) {
        override val bkgColor = Color.TRANSPARENT
    }

    // TODO is it better to use RecyclerView + FlexboxLayoutManager ?
    private val candidatesLayout = FlexboxLayout(ctx).apply {
        horizontalPadding = dp(8)
        flexDirection = FlexDirection.ROW
        flexWrap = FlexWrap.WRAP
        // DividerVertical of FlexboxLayout is the vertical divider line between row of items
        showDividerVertical = FlexboxLayout.SHOW_DIVIDER_MIDDLE
        dividerDrawableVertical = GradientDrawable().apply {
            setSize(dp(4), 0)
        }
    }

    private fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.InputPanelEvent -> {
                inputPanel = it.data
                updateUI()
            }
            is FcitxEvent.PagedCandidateEvent -> {
                paged = it.data
                updateUI()
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

    private fun updateUI() {
        if (evaluateVisibility()) {
            visibility = View.VISIBLE
            preeditUi.update(inputPanel)
            preeditUi.root.isVisible = preeditUi.visible
            updateCandidates()
            updatePosition()
        } else {
            visibility = View.GONE
        }
    }

    private fun updateCandidates() {
        candidatesLayout.removeAllViews()
        candidatesLayout.flexDirection = when (paged.layoutHint) {
            FcitxEvent.PagedCandidateEvent.LayoutHint.Vertical -> FlexDirection.COLUMN
            else -> FlexDirection.ROW
        }
        paged.candidates.forEach {
            val item = CandidateItemUi(ctx, theme).apply {
                text.textSize = 16f
                // TODO different font for comment
                text.text = "${it.label}${it.text} ${it.comment}"
            }
            candidatesLayout.addView(item.root, FlexboxLayout.LayoutParams(-2, -2))
        }
        // TODO paging indicator
    }

    private fun updatePosition() {
        val (horizontal, bottom, top) = anchorPosition
        val (parentWidth, parentHeight) = parentSize
        val selfWidth = width.toFloat()
        val selfHeight = height.toFloat()
        translationX =
            if (horizontal + selfWidth > parentWidth) parentWidth - selfWidth else horizontal
        translationY = if (bottom + selfHeight > parentHeight) top - selfHeight else bottom
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

    private fun setupFcitxEventHandler() {
        eventHandlerJob = service.lifecycleScope.launch {
            fcitx.runImmediately { eventFlow }.collect {
                handleFcitxEvent(it)
            }
        }
    }

    init {
        verticalPadding = dp(8)
        backgroundColor = theme.backgroundColor
        add(preeditUi.root, lParams(wrapContent, wrapContent) {
            topOfParent()
            startOfParent()
        })
        add(candidatesLayout, lParams(wrapContent, wrapContent) {
            below(preeditUi.root)
            startOfParent()
            bottomOfParent()
        })

        layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
    }

    override fun onDetachedFromWindow() {
        handleEvents = false
        super.onDetachedFromWindow()
    }
}
