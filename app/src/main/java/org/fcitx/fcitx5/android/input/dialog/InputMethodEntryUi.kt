/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.dialog

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledDimenPxSize
import splitties.resources.styledDrawable
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.checkedTextView
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter
import splitties.views.textAppearance

// https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r48/core/res/res/layout/input_method_switch_item.xml
class InputMethodEntryUi(override val ctx: Context) : Ui {
    override val root = checkedTextView {
        gravity = gravityVerticalCenter
        minHeight = styledDimenPxSize(android.R.attr.listPreferredItemHeightSmall)
        textAppearance = ctx.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
        setPaddingRelative(
            styledDimenPxSize(android.R.attr.listPreferredItemPaddingStart), 0,
            styledDimenPxSize(android.R.attr.listPreferredItemPaddingEnd), 0
        )
        val radioDrawable = styledDrawable(android.R.attr.listChoiceIndicatorSingle)
        setCompoundDrawablesRelativeWithIntrinsicBounds(radioDrawable, null, null, null)
        compoundDrawablePadding = dp(16)
        ellipsize = TextUtils.TruncateAt.MARQUEE
        background = styledDrawable(android.R.attr.selectableItemBackground)
        isFocusable = true
        isClickable = true
        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
    }
}
