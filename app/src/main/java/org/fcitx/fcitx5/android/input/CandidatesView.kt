/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Size
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
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
import splitties.views.dsl.core.endMargin
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.horizontalPadding
import splitties.views.verticalPadding
import kotlin.math.min

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
    private var candidates = FcitxEvent.CandidateListEvent.Data()

    /**
     * horizontal, bottom, top
     */
    private val anchorPosition = floatArrayOf(0f, 0f, 0f)
    private val parentSize = floatArrayOf(0f, 0f)

    private val preeditUi = object : PreeditUi(ctx, theme) {
        override val bkgColor = Color.TRANSPARENT
    }

    // TODO ui orientation from fcitx
    private val candidatesUi = horizontalLayout {
        horizontalPadding = dp(8)
    }

    private fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            // TODO make a new candidate page event
            is FcitxEvent.CandidateListEvent -> {
                candidates = it.data
                updateUI()
            }
            is FcitxEvent.InputPanelEvent -> {
                inputPanel = it.data
                updateUI()
            }
            else -> {}
        }
    }

    private fun evaluateVisibility(): Boolean {
        return inputPanel.preedit.isNotEmpty() ||
                candidates.total > 0 ||
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
        candidatesUi.apply {
            removeAllViews()
            val limit = min(candidates.candidates.size, 5)
            for (i in 0..<limit) {
                val item = CandidateItemUi(ctx, theme).apply {
                    text.textSize = 16f
                    text.text = "${i + 1}. ${candidates.candidates[i]}"
                }
                addView(item.root, lParams { endMargin = if (i == limit - 1) 0 else dp(8) })
            }
        }
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
        add(candidatesUi, lParams(wrapContent, wrapContent) {
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
