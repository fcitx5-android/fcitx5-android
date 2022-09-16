package org.fcitx.fcitx5.android.input.dialog

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import splitties.dimensions.dp
import splitties.resources.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.checkedTextView
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter
import splitties.views.textAppearance

class InputMethodEntryUi(override val ctx: Context) : Ui {
    override val root = checkedTextView {
        gravity = gravityVerticalCenter
        minHeight = styledDimenPxSize(androidx.appcompat.R.attr.listPreferredItemHeightSmall)
        textAppearance = ctx.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
        setTextColor(styledColor(androidx.appcompat.R.attr.textColorAlertDialogListItem))
        val l = dimenPxSize(androidx.appcompat.R.dimen.abc_select_dialog_padding_start_material)
        val r = styledDimenPxSize(androidx.appcompat.R.attr.dialogPreferredPadding)
        setPadding(l, 0, r, 0)
        setPaddingRelative(l, 0, r, 0)
        val radioDrawable = styledDrawable(android.R.attr.listChoiceIndicatorSingle)
        setCompoundDrawablesWithIntrinsicBounds(radioDrawable, null, null, null)
        setCompoundDrawablesRelativeWithIntrinsicBounds(radioDrawable, null, null, null)
        compoundDrawablePadding = dp(20)
        ellipsize = TextUtils.TruncateAt.MARQUEE
        background = styledDrawable(android.R.attr.selectableItemBackground)
        isFocusable = true
        isClickable = true
        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
    }
}
