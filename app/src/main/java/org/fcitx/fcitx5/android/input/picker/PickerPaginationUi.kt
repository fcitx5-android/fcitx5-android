/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.utils.alpha
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import kotlin.math.roundToInt

class PickerPaginationUi(override val ctx: Context, val theme: Theme) : Ui {

    private val highlight = view(::View) {
        backgroundColor = theme.keyTextColor
    }

    override val root = constraintLayout {
        backgroundColor = theme.keyTextColor.alpha(0.3f)
    }

    private var pageCount: Int = 0

    fun updatePageCount(value: Int) {
        if (pageCount == value) return
        if (value <= 1) {
            // there will be only one page : remove highlight
            root.removeView(highlight)
        } else if (pageCount <= 1) {
            // incoming count > 1 but current count <= 1 : add highlight
            root.apply {
                add(highlight, lParams(matchConstraints, matchParent) {
                    centerVertically()
                    startOfParent()
                    matchConstraintPercentWidth = 1f / value
                })
            }
        } else {
            // both count >= 1 : update highlight width
            highlight.updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintPercentWidth = 1f / value
            }
        }
        pageCount = value
    }

    fun updateScrollProgress(current: Int, progress: Float) {
        if (pageCount <= 1) {
            return
        }
        highlight.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = ((current + progress) * highlight.width).roundToInt()
        }
    }
}
