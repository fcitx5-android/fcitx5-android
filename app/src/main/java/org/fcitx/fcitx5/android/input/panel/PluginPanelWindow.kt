/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.panel

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.TextView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent

/**
 * Host-side window hosting an interactive input panel plugin.
 *
 * The window consists of:
 * - a [SurfaceView] whose [Surface][android.view.Surface] is handed over to the
 *   plugin, so that the plugin can render its own UI (e.g. a handwriting canvas)
 *   from its own process;
 * - a host-side candidate bar showing candidates published by the plugin via
 *   [InteractivePanelManager]; tapping a candidate commits its text.
 *
 * Touch events on the surface are forwarded to the plugin, so the plugin can
 * handle strokes / gestures without the host knowing their meaning.
 */
@SuppressLint("ClickableViewAccessibility")
class PluginPanelWindow : InputWindow.ExtendedInputWindow<PluginPanelWindow>() {

    private val panelManager: InteractivePanelManager by manager.must()
    private val theme: Theme by manager.theme()

    override val title: String
        get() = context.getString(R.string.plugin_panel)

    override val showTitle: Boolean = true

    private val surfaceView: SurfaceView by lazy {
        SurfaceView(context).apply {
            setOnTouchListener { _, event ->
                handleTouch(event)
            }
        }
    }

    private val holderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            panelManager.onSurfaceCreated(
                holder.surface,
                holder.surfaceFrame.width(),
                holder.surfaceFrame.height()
            )
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            panelManager.onSizeChanged(width, height)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            panelManager.onSurfaceDestroyed()
        }
    }

    private val candidateBar by lazy {
        HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(
                candidateContainer,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private val candidateContainer by lazy {
        context.horizontalLayout {
            // vertical center
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
    }

    override fun onCreateView(): View {
        return context.frameLayout {
            backgroundColor = theme.keyboardColor
            add(surfaceView, lParams(matchParent, matchParent))
        }
    }

    /**
     * The candidate bar is hosted in the title bar (next to the "Plugin panel" title),
     * so that the whole window below is left to the plugin's canvas.
     */
    override fun onCreateBarExtension(): View? = candidateBar

    override fun onAttached() {
        candidateContainer.removeAllViews()
        surfaceView.holder.addCallback(holderCallback)
        panelManager.candidatesListener = InteractivePanelManager.OnCandidatesListener { candidates ->
            updateCandidates(candidates)
        }
        panelManager.attachPanelWindow()
    }

    override fun onDetached() {
        panelManager.candidatesListener = null
        panelManager.detachPanelWindow()
        surfaceView.holder.removeCallback(holderCallback)
    }

    private fun updateCandidates(candidates: List<String>) {
        candidateContainer.removeAllViews()
        candidates.forEach { candidate ->
            candidateContainer.addView(
                newCandidateChip(candidate),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = context.dp(8)
                }
            )
        }
        candidateBar.fullScroll(HorizontalScrollView.FOCUS_LEFT)
    }

    private fun newCandidateChip(text: String): TextView {
        return context.textView {
            this.text = text
            setTextColor(theme.candidateTextColor)
            textSize = 16f
            gravity = Gravity.CENTER
            backgroundColor = theme.keyBackgroundColor
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener {
                panelManager.commitText(text)
            }
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                panelManager.onTouchDown(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val historySize = event.historySize
                val xs = FloatArray(historySize + 1)
                val ys = FloatArray(historySize + 1)
                for (i in 0 until historySize) {
                    xs[i] = event.getHistoricalX(i)
                    ys[i] = event.getHistoricalY(i)
                }
                xs[historySize] = event.x
                ys[historySize] = event.y
                panelManager.onTouchMove(xs, ys)
                return true
            }
            MotionEvent.ACTION_UP -> {
                panelManager.onTouchUp(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                // treat cancel as stroke end; plugin may discard the stroke
                panelManager.onTouchUp(event.x, event.y)
                return true
            }
            else -> return false
        }
    }
}
