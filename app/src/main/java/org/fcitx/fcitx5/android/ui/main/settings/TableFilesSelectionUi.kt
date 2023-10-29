/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDimenPxSize
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.textAppearance
import splitties.views.topPadding

class TableFilesSelectionUi(override val ctx: Context) : Ui {

    inner class FileSelectionUi(@StringRes titleRes: Int) : Ui {
        override val ctx: Context
            get() = this@TableFilesSelectionUi.ctx

        val title = textView {
            textAppearance = ctx.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
            setTextColor(styledColor(android.R.attr.textColorPrimary))
            setText(titleRes)
        }

        val summary = textView {
            textAppearance = ctx.resolveThemeAttribute(android.R.attr.textAppearanceSmall)
            setTextColor(styledColor(android.R.attr.textColorSecondary))
            setText(R.string.table_file_placeholder)
        }

        override val root = constraintLayout {
            isFocusable = true
            isClickable = true
            background = styledDrawable(android.R.attr.selectableItemBackground)
            val hPadding = styledDimenPxSize(android.R.attr.dialogPreferredPadding)
            val vPadding = dp(16)
            setPaddingRelative(hPadding, vPadding, hPadding, vPadding)
            minHeight = styledDimenPxSize(android.R.attr.listPreferredItemHeight)
            add(title, lParams(wrapContent, wrapContent) {
                topOfParent()
                startOfParent()
                above(summary)
            })
            add(summary, lParams(wrapContent, wrapContent) {
                below(title)
                startOfParent()
                bottomOfParent()
            })
        }
    }

    val conf = FileSelectionUi(R.string.table_conf_select_title)

    val dict = FileSelectionUi(R.string.table_dict_select_title)

    override val root = constraintLayout {
        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
        topPadding = dp(8)
        add(conf.root, lParams(matchParent, wrapContent) {
            topOfParent()
            centerHorizontally()
            above(dict.root)
        })
        add(dict.root, lParams(matchParent, wrapContent) {
            below(conf.root)
            centerHorizontally()
            bottomOfParent()
        })
    }

    fun reset() {
        val placeholderText = ctx.getString(R.string.table_file_placeholder)
        conf.summary.text = placeholderText
        dict.summary.text = placeholderText
    }
}
