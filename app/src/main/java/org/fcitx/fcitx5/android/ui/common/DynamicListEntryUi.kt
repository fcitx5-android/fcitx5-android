package org.fcitx.fcitx5.android.ui.common

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDimenPxSize
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource
import splitties.views.setPaddingDp
import splitties.views.textAppearance

class DynamicListEntryUi(override val ctx: Context) : Ui {

    val handleImage = imageView {
        imageResource = R.drawable.ic_baseline_drag_handle_24
        colorFilter = PorterDuffColorFilter(styledColor(android.R.attr.colorAccent), PorterDuff.Mode.SRC_IN)
    }

    val checkBox = checkBox()

    val nameText = textView {
        setPaddingDp(0, 16, 0, 16)
        textAppearance = ctx.resolveThemeAttribute(androidx.appcompat.R.attr.textAppearanceListItem)
    }

    val editButton = imageButton {
        background = styledDrawable(android.R.attr.selectableItemBackground)
        imageResource = R.drawable.ic_baseline_edit_24
    }

    val settingsButton = imageButton {
        background = styledDrawable(android.R.attr.selectableItemBackground)
        imageResource = R.drawable.ic_baseline_settings_24
    }

    override val root: View = constraintLayout {
        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
        backgroundColor = styledColor(android.R.attr.colorBackground)
        minHeight = styledDimenPxSize(android.R.attr.listPreferredItemHeightSmall)

        add(handleImage, lParams {
            width = dp(24)
            height = 0
            centerVertically()
            startOfParent(styledDimenPxSize(android.R.attr.listPreferredItemPaddingStart))
        })

        add(checkBox, lParams {
            width = dp(30)
            height = 0
            centerVertically()
            after(handleImage, dp(12))
        })

        add(nameText, lParams {
            width = 0
            height = wrapContent
            centerVertically()
            before(editButton, dp(12))
            after(checkBox, dp(16))
        })

        add(editButton, lParams {
            width = dp(53)
            height = 0
            centerVertically()
            before(settingsButton)
            after(nameText)
        })

        add(settingsButton, lParams {
            width = dp(53)
            height = 0
            centerVertically()
            endOfParent()
        })
    }
}