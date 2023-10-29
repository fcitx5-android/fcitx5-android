/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.graphics.Color
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable

class NewThemeEntryUi(override val ctx: Context) : Ui {
    val text = textView {
        setText(R.string.new_theme)
        setTextColor(Color.WHITE)
    }

    val icon = imageView {
        imageDrawable = ctx.drawable(R.drawable.ic_baseline_plus_24)!!.apply {
            setTint(Color.WHITE)
        }
    }

    override val root = constraintLayout {
        foreground = styledDrawable(android.R.attr.selectableItemBackground)
        background = ctx.drawable(R.drawable.bkg_theme_choose_image)
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = dp(2f)
        add(icon, lParams(dp(24), dp(24)) {
            topOfParent()
            centerHorizontally()
            above(text, dp(4))
            verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
        })
        add(text, lParams(wrapContent, wrapContent) {
            below(icon)
            centerHorizontally()
            bottomOfParent()
        })
    }
}
