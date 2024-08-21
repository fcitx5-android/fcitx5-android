/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.AlignItems
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
import splitties.resources.drawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.horizontalPadding
import splitties.views.imageDrawable
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

    private var shouldUpdatePosition = false

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
        // update position after layout child views and before drawing to avoid flicker
        viewTreeObserver.addOnPreDrawListener {
            if (shouldUpdatePosition) {
                updatePosition()
            }
            true
        }
    }

    private fun createIcon(@DrawableRes icon: Int) = imageView {
        imageTintList = ColorStateList.valueOf(theme.keyTextColor)
        imageDrawable = drawable(icon)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val prevIcon = createIcon(R.drawable.ic_baseline_arrow_left_24)
    private val nextIcon = createIcon(R.drawable.ic_baseline_arrow_right_24)

    private val paginationLayout = constraintLayout {
        add(nextIcon, lParams(dp(10), matchConstraints) {
            centerVertically()
            endOfParent()
        })
        add(prevIcon, lParams(dp(10), matchConstraints) {
            centerVertically()
            before(nextIcon)
        })
        layoutParams = FlexboxLayout.LayoutParams(wrapContent, wrapContent).apply {
            flexGrow = 1f
            alignSelf = AlignItems.STRETCH
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
            preeditUi.update(inputPanel)
            preeditUi.root.visibility = if (preeditUi.visible) View.VISIBLE else View.GONE
            updateCandidates()
            visibility = View.VISIBLE
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
        paged.candidates.forEachIndexed { i, it ->
            val item = CandidateItemUi(ctx, theme).apply {
                text.textSize = 16f
                // TODO different font for comment
                text.text = "${it.label}${it.text} ${it.comment}"
                if (i == paged.cursorIndex) {
                    root.backgroundColor = theme.genericActiveBackgroundColor
                    text.setTextColor(theme.genericActiveForegroundColor)
                }
            }
            candidatesLayout.addView(item.root, FlexboxLayout.LayoutParams(-2, -2))
        }
        if (paged.hasPrev || paged.hasNext) {
            prevIcon.alpha = if (paged.hasPrev) 1f else 0.4f
            nextIcon.alpha = if (paged.hasNext) 1f else 0.4f
            candidatesLayout.addView(paginationLayout)
        }
        shouldUpdatePosition = true
    }

    private fun updatePosition() {
        val (horizontal, bottom, top) = anchorPosition
        val (parentWidth, parentHeight) = parentSize
        val selfWidth = width.toFloat()
        val selfHeight = height.toFloat()
        translationX =
            if (horizontal + selfWidth > parentWidth) parentWidth - selfWidth else horizontal
        translationY = if (bottom + selfHeight > parentHeight) top - selfHeight else bottom
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
