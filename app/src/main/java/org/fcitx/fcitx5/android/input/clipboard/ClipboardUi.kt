/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ViewAnimator
import androidx.transition.Fade
import androidx.transition.TransitionManager
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.ClipboardCategory
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.coordinatorlayout.coordinatorLayout
import splitties.views.dsl.coordinatorlayout.defaultLParams
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityCenter
import splitties.views.setPaddingDp
import timber.log.Timber

class ClipboardUi(override val ctx: Context, private val theme: Theme) : Ui {

    val recyclerView = recyclerView {
        addItemDecoration(SpacesItemDecoration(dp(4)))
    }

    val enableUi = ClipboardInstructionUi.Enable(ctx, theme)

    val emptyUi = ClipboardInstructionUi.Empty(ctx, theme)

    val viewAnimator = view(::ViewAnimator) {
        add(recyclerView, lParams(matchParent, matchParent))
        add(emptyUi.root, lParams(matchParent, matchParent))
        add(enableUi.root, lParams(matchParent, matchParent))
    }

    private val categoryButtons = linkedMapOf(
        ClipboardCategory.All to createCategoryButton(R.string.clipboard_category_all),
        ClipboardCategory.Favorites to createCategoryButton(R.string.clipboard_category_favorites),
        ClipboardCategory.Local to createCategoryButton(R.string.clipboard_category_local),
        ClipboardCategory.Media to createCategoryButton(R.string.clipboard_category_media),
        ClipboardCategory.Remote to createCategoryButton(R.string.clipboard_category_remote)
    )

    val categoryBar = view(::HorizontalScrollView) {
        isHorizontalScrollBarEnabled = false
        add(horizontalLayout {
            setPaddingDp(8, 8, 8, 4)
            categoryButtons.forEach { (_, button) ->
                add(
                    button,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(30)
                    ).apply { rightMargin = dp(6) }
                )
            }
        }, lParams(wrapContent, matchParent))
    }

    private val keyBorder by ThemeManager.prefs.keyBorder
    private val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    override val root = coordinatorLayout {
        if (!keyBorder) {
            backgroundColor = theme.barColor
        }
        add(verticalLayout {
            add(categoryBar, LinearLayout.LayoutParams(matchParent, LinearLayout.LayoutParams.WRAP_CONTENT))
            add(
                viewAnimator,
                LinearLayout.LayoutParams(matchParent, 0).apply { weight = 1f }
            )
        }, defaultLParams(matchParent, matchParent))
    }

    val deleteAllButton = ToolButton(ctx, R.drawable.ic_baseline_delete_sweep_24, theme).apply {
        contentDescription = ctx.getString(R.string.delete_all)
    }

    val extension = horizontalLayout {
        add(deleteAllButton, lParams(dp(40), dp(40)))
    }

    private fun setDeleteButtonShown(enabled: Boolean) {
        deleteAllButton.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    private fun createCategoryButton(textRes: Int) = textView {
        gravity = gravityCenter
        minWidth = dp(64)
        text = ctx.getString(textRes)
        textSize = 13f
        setPaddingDp(12, 0, 12, 0)
        background = categoryBackground(selected = false)
        setTextColor(theme.keyTextColor)
    }

    private fun categoryBackground(selected: Boolean) = RippleDrawable(
        ColorStateList.valueOf(theme.keyPressHighlightColor),
        GradientDrawable().apply {
            cornerRadius = ctx.dp(15).toFloat()
            setColor(if (selected) theme.genericActiveBackgroundColor else theme.clipboardEntryColor)
        },
        GradientDrawable().apply {
            cornerRadius = ctx.dp(15).toFloat()
            setColor(Color.WHITE)
        }
    )

    fun setOnCategorySelectedListener(listener: (ClipboardCategory) -> Unit) {
        categoryButtons.forEach { (category, button) ->
            button.setOnClickListener {
                setSelectedCategory(category)
                listener(category)
            }
        }
    }

    fun setSelectedCategory(category: ClipboardCategory) {
        categoryButtons.forEach { (buttonCategory, button) ->
            val selected = buttonCategory == category
            button.background = categoryBackground(selected)
            button.setTextColor(if (selected) theme.altKeyTextColor else theme.keyTextColor)
        }
    }

    fun switchUiByState(state: ClipboardStateMachine.State) {
        Timber.d("Switch clipboard to $state")
        if (!disableAnimation)
            TransitionManager.beginDelayedTransition(root, Fade().apply { duration = 100L })
        when (state) {
            ClipboardStateMachine.State.Normal -> {
                viewAnimator.displayedChild = 0
                setDeleteButtonShown(true)
            }

            ClipboardStateMachine.State.AddMore -> {
                viewAnimator.displayedChild = 1
                setDeleteButtonShown(false)
            }

            ClipboardStateMachine.State.EnableListening -> {
                viewAnimator.displayedChild = 2
                setDeleteButtonShown(false)
            }
        }
    }
}
