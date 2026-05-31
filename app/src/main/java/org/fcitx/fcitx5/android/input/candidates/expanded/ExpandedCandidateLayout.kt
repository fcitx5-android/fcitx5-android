/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.BackspaceKey
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.ImageKeyView
import org.fcitx.fcitx5.android.input.keyboard.ImageLayoutSwitchKey
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.keyboard.ReturnKey
import org.fcitx.fcitx5.android.utils.setVerticalScrollbarThumbColor
import org.fcitx.fcitx5.android.utils.singleSideBorderDrawable
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.leftToRightOf
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.constraintlayout.rightToLeftOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource

@SuppressLint("ViewConstructor")
class ExpandedCandidateLayout(context: Context, theme: Theme) : ConstraintLayout(context) {

    class Keyboard(context: Context, theme: Theme) : BaseKeyboard(context, theme, Layout) {
        companion object {
            const val UpBtnLabel = "U"
            const val DownBtnLabel = "D"

            const val UpBtnId = 0xff55
            const val DownBtnId = 0xff56

            val Layout: List<List<KeyDef>> = listOf(
                listOf(
                    ImageLayoutSwitchKey(
                        R.drawable.ic_baseline_arrow_upward_24,
                        to = UpBtnLabel,
                        percentWidth = 1f,
                        variant = KeyDef.Appearance.Variant.Alternative,
                        viewId = UpBtnId
                    )
                ),
                listOf(
                    ImageLayoutSwitchKey(
                        R.drawable.ic_baseline_arrow_downward_24,
                        to = DownBtnLabel,
                        percentWidth = 1f,
                        variant = KeyDef.Appearance.Variant.Alternative,
                        viewId = DownBtnId
                    )
                ),
                listOf(BackspaceKey(percentWidth = 1f, KeyDef.Appearance.Variant.Alternative)),
                listOf(ReturnKey(percentWidth = 1f))
            )
        }

        val pageUpBtn: ImageKeyView by lazy { findViewById(UpBtnId) }
        val pageDnBtn: ImageKeyView by lazy { findViewById(DownBtnId) }
        val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
        val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

        override fun onReturnDrawableUpdate(returnDrawable: Int) {
            `return`.img.imageResource = returnDrawable
        }
    }

    private val keyBorder by ThemeManager.prefs.keyBorder
    private val keyVerticalMargin by ThemeManager.prefs.keyVerticalMargin
    private val keyVerticalMarginLandscape by ThemeManager.prefs.keyVerticalMarginLandscape

    val recyclerView = recyclerView {
        // disable item cross-fade animation
        itemAnimator = null
        isVerticalScrollBarEnabled = false
    }

    val tabsContainer = constraintLayout()

    val scrollableTabs = recyclerView {
        itemAnimator = null
        // always show scrollbar
        isScrollbarFadingEnabled = false
        scrollBarSize = dp(1)
        setVerticalScrollbarThumbColor(theme.candidateTextColor)
    }

    val pinnedTabs = recyclerView {
        itemAnimator = null
        // prevent scrolling in pinned tabs at bottom
        overScrollMode = OVER_SCROLL_NEVER
        isNestedScrollingEnabled = false
        isVerticalScrollBarEnabled = false
    }

    var pageUpBtn: ImageKeyView
    var pageDnBtn: ImageKeyView

    val embeddedKeyboard = Keyboard(context, theme).also {
        pageUpBtn = it.pageUpBtn
        pageDnBtn = it.pageDnBtn
    }

    init {
        id = R.id.expanded_candidate_view

        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val inset = dp(if (landscape) keyVerticalMarginLandscape else keyVerticalMargin)
        tabsContainer.background =
            singleSideBorderDrawable(dp(1), theme.dividerColor, Gravity.RIGHT, inset)
        if (!keyBorder) {
            backgroundColor = theme.barColor
            embeddedKeyboard.background =
                singleSideBorderDrawable(dp(1), theme.dividerColor, Gravity.LEFT, inset)
        }

        add(tabsContainer, lParams {
            matchConstraintPercentWidth = 0.15f
            topOfParent()
            leftOfParent()
            bottomOfParent()
        })
        tabsContainer.apply {
            add(scrollableTabs, lParams {
                topOfParent()
                centerHorizontally()
                above(pinnedTabs)
            })
            add(pinnedTabs, lParams(height = wrapContent) {
                bottomOfParent()
                centerHorizontally()
            })
        }
        add(recyclerView, lParams {
            topOfParent()
            leftToRightOf(tabsContainer)
            rightToLeftOf(embeddedKeyboard)
            bottomOfParent()
        })
        add(embeddedKeyboard, lParams {
            matchConstraintPercentWidth = 0.15f
            topOfParent()
            leftToRightOf(recyclerView)
            rightOfParent()
            bottomOfParent()
        })
    }

    fun resetPosition() {
        recyclerView.scrollToPosition(0)
    }
}