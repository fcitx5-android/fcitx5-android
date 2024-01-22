/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.content.Context
import android.os.Build
import android.view.SurfaceControl
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import androidx.core.view.updateLayoutParams
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.centerOn
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.horizontalMargin
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent

class InlineSuggestionsUi(override val ctx: Context) : Ui {

    private val scrollView = ctx.view(::HorizontalScrollView) {
        isFillViewport = true
        scrollBarSize = dp(1)
    }

    private val scrollSurfaceView = ctx.view(::SurfaceView) {
        setZOrderOnTop(true)
    }

    private val scrollableContentViews = mutableListOf<InlineContentView>()

    private val pinnedView = frameLayout { }

    private var pinnedContentView: InlineContentView? = null

    override val root = constraintLayout {
        add(scrollView, lParams(matchConstraints, matchParent) {
            startOfParent()
            before(pinnedView)
            centerVertically()
        })
        add(scrollSurfaceView, lParams(matchConstraints, matchParent) {
            centerOn(scrollView)
        })
        add(pinnedView, lParams(wrapContent, matchParent) {
            endOfParent()
            centerVertically()
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun clearScrollView() {
        scrollView.scrollTo(0, 0)
        scrollView.removeAllViews()
        scrollableContentViews.forEach { v ->
            v.surfaceControl?.let { sc ->
                SurfaceControl.Transaction().reparent(sc, null).apply()
            }
        }
        scrollableContentViews.clear()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun clearPinnedView() {
        pinnedView.removeAllViews()
        pinnedContentView = null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clear() {
        clearScrollView()
        clearPinnedView()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setPinnedView(view: InlineContentView?) {
        pinnedView.removeAllViews()
        pinnedContentView = view?.also {
            pinnedView.addView(it)
            it.updateLayoutParams<FrameLayout.LayoutParams> {
                horizontalMargin = ctx.dp(10)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setScrollableViews(views: List<InlineContentView?>) {
        val flexbox = view(::FlexboxLayout) {
            flexWrap = FlexWrap.NOWRAP
            justifyContent = JustifyContent.CENTER
        }
        val parentSurfaceControl = scrollSurfaceView.surfaceControl
        views.forEach {
            if (it == null) return@forEach
            scrollableContentViews.add(it)
            it.setSurfaceControlCallback(object : InlineContentView.SurfaceControlCallback {
                override fun onCreated(surfaceControl: SurfaceControl) {
                    SurfaceControl.Transaction()
                        .reparent(surfaceControl, parentSurfaceControl)
                        .apply()
                }

                override fun onDestroyed(surfaceControl: SurfaceControl) {}
            })
            flexbox.addView(it)
            it.updateLayoutParams<FlexboxLayout.LayoutParams> {
                flexShrink = 0f
            }
        }
        scrollView.apply {
            scrollTo(0, 0)
            removeAllViews()
            add(flexbox, lParams(wrapContent, matchParent))
        }
    }
}
