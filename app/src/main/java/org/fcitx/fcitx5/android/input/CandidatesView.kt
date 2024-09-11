/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.CandidateItemUi
import org.fcitx.fcitx5.android.input.preedit.PreeditUi
import org.fcitx.fcitx5.android.utils.styledFloat
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
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
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
                if (eventHandlerJob == null) {
                    setupFcitxEventHandler()
                }
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

    inner class UiViewHolder(val ui: Ui) : RecyclerView.ViewHolder(ui.root)

    private val candidatesAdapter = object : RecyclerView.Adapter<UiViewHolder>() {
        override fun getItemCount() =
            paged.candidates.size + (if (paged.hasPrev || paged.hasNext) 1 else 0)

        override fun getItemViewType(position: Int) = if (position < paged.candidates.size) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UiViewHolder {
            return when (viewType) {
                0 -> UiViewHolder(CandidateItemUi(ctx, theme).apply {
                    text.textSize = 16f
                })
                else -> UiViewHolder(object : Ui {
                    override val ctx = this@CandidatesView.ctx
                    override val root = this@CandidatesView.paginationLayout
                })
            }
        }

        override fun onBindViewHolder(holder: UiViewHolder, position: Int) {
            when (getItemViewType(position)) {
                0 -> {
                    holder.ui as CandidateItemUi
                    val candidate = paged.candidates[position]
                    // TODO different font for comment
                    holder.ui.text.text = "${candidate.label}${candidate.text} ${candidate.comment}"
                    if (position == paged.cursorIndex) {
                        holder.ui.text.setTextColor(theme.genericActiveForegroundColor)
                        holder.ui.root.backgroundColor = theme.genericActiveBackgroundColor
                    } else {
                        holder.ui.text.setTextColor(theme.keyTextColor)
                        holder.ui.root.backgroundColor = Color.TRANSPARENT
                    }
                }
                else -> {
                    prevIcon.alpha = if (paged.hasPrev) 1f else 0.4f
                    nextIcon.alpha = if (paged.hasNext) 1f else 0.4f
                }
            }
        }
    }

    private val candidatesLayoutManager = object : FlexboxLayoutManager(ctx) {
        init {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            alignItems = AlignItems.FLEX_START
        }

        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            shouldUpdatePosition = true
        }
    }

    private val updatePositionListener = OnPreDrawListener {
        if (shouldUpdatePosition) {
            updatePosition()
        }
        true
    }

    private val recyclerView = recyclerView {
        isFocusable = false
        horizontalPadding = dp(8)
        adapter = candidatesAdapter
        layoutManager = candidatesLayoutManager
        // update position after layout child views and before drawing to avoid flicker
        this@CandidatesView.viewTreeObserver.addOnPreDrawListener(updatePositionListener)
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
        layoutParams = FlexboxLayoutManager.LayoutParams(wrapContent, wrapContent).apply {
            flexGrow = 1f
            alignSelf = AlignItems.STRETCH
            minHeight = dp(20)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCandidates() {
        candidatesLayoutManager.flexDirection = when (paged.layoutHint) {
            FcitxEvent.PagedCandidateEvent.LayoutHint.Vertical -> FlexDirection.COLUMN
            else -> FlexDirection.ROW
        }
        candidatesAdapter.notifyDataSetChanged()
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
        add(recyclerView, lParams(wrapContent, wrapContent) {
            below(preeditUi.root)
            startOfParent()
            bottomOfParent()
        })

        isFocusable = false
        layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
    }

    override fun onDetachedFromWindow() {
        handleEvents = false
        viewTreeObserver.removeOnPreDrawListener(updatePositionListener)
        super.onDetachedFromWindow()
    }
}
