/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.expanded

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.Gravity
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.core.CandidateAction
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.gravityCenter

abstract class CandidateTabActionsAdapter(val theme: Theme, val indicatorStyle: Boolean = false) :
    RecyclerView.Adapter<CandidateTabActionsAdapter.TabActionViewHolder>() {

    private var tabs = listOf<CandidateAction>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateTabs(newTabs: List<CandidateAction>) {
        tabs = newTabs
        notifyDataSetChanged()
    }

    abstract fun onTriggerTabAction(id: Int)

    override fun getItemCount() = tabs.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabActionViewHolder {
        val ui = TabActionUi(parent.context, theme, indicatorStyle)
        ui.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return TabActionViewHolder(ui)
    }

    override fun onBindViewHolder(holder: TabActionViewHolder, position: Int) {
        val tab = tabs.getOrNull(position) ?: return
        holder.update(tab)
    }

    override fun onViewRecycled(holder: TabActionViewHolder) {
        holder.clear()
    }

    inner class TabActionViewHolder(val ui: TabActionUi) : RecyclerView.ViewHolder(ui.root) {
        var tabAction: CandidateAction? = null
            private set

        fun update(tab: CandidateAction) {
            tabAction = tab
            ui.update(tab)
            ui.root.setOnClickListener {
                onTriggerTabAction(tab.id)
            }
        }

        fun clear() {
            tabAction = null
            ui.root.setOnClickListener(null)
        }
    }

    class TabActionUi(
        override val ctx: Context,
        val theme: Theme,
        val indicatorStyle: Boolean = false
    ) : Ui {

        private val label = view(::AutoScaleTextView) {
            scaleMode = AutoScaleTextView.Mode.Proportional
            textSize = 16f // sp
            isSingleLine = true
            gravity = gravityCenter
            setTextColor(theme.candidateTextColor)
        }

        override val root = view(::CustomGestureView) {
            background = createBackground(false, indicatorStyle)
            add(label, lParams(matchParent, ctx.dp(36)) {
                gravity = gravityCenter
            })
        }

        var checkable = false
            set(value) {
                if (field == value) return
                field = value
                root.background = createBackground(value, indicatorStyle)
            }

        fun update(tab: CandidateAction) {
            label.text = tab.text
            checkable = tab.isCheckable
            root.isActivated = tab.isChecked
        }

        private fun createBackground(checkable: Boolean, indicatorStyle: Boolean): Drawable {
            if (!checkable) {
                return pressHighlightDrawable(theme.keyPressHighlightColor)
            }
            if (!indicatorStyle) {
                val highlight = ColorDrawable(theme.keyPressHighlightColor)
                return StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), highlight)
                    addState(intArrayOf(android.R.attr.state_activated), highlight)
                }
            }
            val states = arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf()
            )
            val colors = intArrayOf(
                theme.accentKeyBackgroundColor,
                theme.altKeyBackgroundColor
            )
            return LayerDrawable(
                arrayOf(
                    ShapeDrawable(RectShape()).apply {
                        setTintList(ColorStateList(states, colors))
                    },
                    pressHighlightDrawable(theme.keyPressHighlightColor)
                )
            ).apply {
                setLayerSize(0, ctx.dp(11), ctx.dp(2))
                setLayerGravity(0, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                setLayerInsetBottom(0, ctx.dp(4))
            }
        }
    }
}
