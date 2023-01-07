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
        colorFilter =
            PorterDuffColorFilter(styledColor(android.R.attr.colorAccent), PorterDuff.Mode.SRC_IN)
        setPaddingDp(3, 0, 3, 0)
    }

    val multiselectCheckBox = checkBox()

    val checkBox = checkBox()

    val nameText = textView {
        setPaddingDp(0, 16, 0, 16)
        textAppearance = ctx.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
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

        val paddingStart = styledDimenPxSize(android.R.attr.listPreferredItemPaddingStart)

        add(handleImage, lParams {
            width = dp(30)
            height = 0
            centerVertically()
            startOfParent(paddingStart)
        })

        add(multiselectCheckBox, lParams {
            width = dp(30)
            height = 0
            centerVertically()
            after(handleImage, paddingStart)
        })

        add(checkBox, lParams {
            width = dp(30)
            height = 0
            centerVertically()
            after(multiselectCheckBox, paddingStart)
        })

        add(nameText, lParams {
            width = 0
            height = wrapContent
            centerVertically()
            after(checkBox, paddingStart)
            before(editButton)
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