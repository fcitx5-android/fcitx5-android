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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.CandidateSource
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateViewAdapter
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

    /**
     * Candidates published by the plugin, rendered with the shared
     * [HorizontalCandidateViewAdapter] in the title bar.
     */
    private val pluginCandidates = mutableListOf<CandidateWord>()

    private val candidateSource = object : CandidateSource {
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

    private val candidateAdapter by lazy {
        object : HorizontalCandidateViewAdapter(theme) {
            override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                holder.itemView.setOnClickListener {
                    candidateSource.onCandidateClick(holder.idx)
                }
                holder.itemView.setOnLongClickListener {
                    candidateSource.onCandidateLongClick(holder.idx, holder.ui.root)
                }
            }

            override fun onViewRecycled(holder: CandidateViewHolder) {
                holder.itemView.setOnClickListener(null)
                holder.itemView.setOnLongClickListener(null)
                super.onViewRecycled(holder)
            }
        }
    }

    private val candidateBar by lazy {
        RecyclerView(context).apply {
            isHorizontalScrollBarEnabled = false
            itemAnimator = null
            adapter = candidateAdapter
            layoutManager = FlexboxLayoutManager(context)
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
        candidateAdapter.updateCandidates(pluginCandidates.toTypedArray(), pluginCandidates.size)
        candidateBar.scrollToPosition(0)
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
