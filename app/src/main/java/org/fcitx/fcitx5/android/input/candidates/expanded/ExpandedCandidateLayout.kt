/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import android.annotation.SuppressLint
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.*
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
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

    val recyclerView = recyclerView {
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
        if (!keyBorder) {
            backgroundColor = theme.barColor
        }

        add(recyclerView, lParams {
            topOfParent()
            leftOfParent()
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