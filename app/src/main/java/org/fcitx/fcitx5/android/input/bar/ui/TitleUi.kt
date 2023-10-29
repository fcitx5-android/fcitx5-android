/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.graphics.Typeface
import android.view.View
import androidx.core.view.isVisible
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter

class TitleUi(override val ctx: Context, theme: Theme) : Ui {

    private val backButton = ToolButton(ctx, R.drawable.ic_baseline_arrow_back_24, theme)

    private val titleText = textView {
        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        setTextColor(theme.altKeyTextColor)
        gravity = gravityVerticalCenter
        textSize = 16f
    }

    private var extension: View? = null

    override val root = constraintLayout {
        add(backButton, lParams(dp(40), dp(40)) {
            topOfParent()
            startOfParent()
            bottomOfParent()
        })
        add(titleText, lParams(wrapContent, dp(40)) {
            topOfParent()
            after(backButton, dp(8))
            bottomOfParent()
        })
    }

    fun setReturnButtonOnClickListener(block: () -> Unit) {
        backButton.setOnClickListener {
            block()
        }
    }

    fun setTitle(title: String) {
        titleText.text = title
    }

    fun addExtension(view: View, showTitle: Boolean) {
        if (extension != null) {
            throw IllegalStateException("TitleBar extension is already present")
        }
        backButton.isVisible = showTitle
        titleText.isVisible = showTitle
        extension = view
        root.run {
            add(view, lParams(matchConstraints, dp(40)) {
                centerVertically()
                if (showTitle) {
                    endOfParent(dp(5))
                } else {
                    centerHorizontally()
                }
            })
        }
    }

    fun removeExtension() {
        extension?.let {
            root.removeView(it)
            extension = null
        }
    }
}
