/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.panel

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.CandidateSource
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent

/**
 * Host-side window hosting an interactive input panel plugin.
 *
 * The window consists of a [SurfaceView] whose [Surface][android.view.Surface]
 * is handed over to the plugin, so that the plugin can render its own UI
 * (e.g. a handwriting canvas) from its own process.
 *
 * Candidates published by the plugin are displayed by the host in the shared
 * candidate bar of the Kawaii bar (via [candidateSource] and
 * [onCandidatesPublished]), so the Kawaii bar itself is not replaced by a
 * plugin-specific title bar.
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

    /**
     * Candidates published by the plugin, rendered by the host in the shared
     * candidate bar of the Kawaii bar.
     */
    private val pluginCandidates = mutableListOf<CandidateWord>()

    /**
     * Candidate source handed to the host candidate bar while this window is
     * attached; tapping a candidate commits its text.
     */
    val candidateSource = object : CandidateSource {
        override val candidates: List<CandidateWord>
            get() = pluginCandidates

        override val total: Int
            get() = pluginCandidates.size

        override fun onCandidateClick(idx: Int) {
            pluginCandidates.getOrNull(idx)?.let {
                panelManager.commitText(it.text)
            }
        }

        override fun onCandidateLongClick(idx: Int, view: View): Boolean = false
    }

    /**
     * Invoked by the host when candidates are published, so the host can
     * refresh the shared candidate bar. Set by [KawaiiBarComponent] when this
     * window is attached.
     */
    var onCandidatesPublished: ((List<CandidateWord>) -> Unit)? = null

    override fun onCreateView(): View {
        return context.frameLayout {
            backgroundColor = theme.keyboardColor
            add(surfaceView, lParams(matchParent, matchParent))
        }
    }

    override fun onAttached() {
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
        pluginCandidates.clear()
        pluginCandidates.addAll(candidates.map { CandidateWord("", it, "") })
        onCandidatesPublished?.invoke(pluginCandidates.toList())
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
