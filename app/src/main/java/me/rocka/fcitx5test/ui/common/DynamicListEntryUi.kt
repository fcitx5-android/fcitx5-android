package me.rocka.fcitx5test.ui.common

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
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
        minHeight = dp(60)
        setPaddingDp(0, 18, 0, 18)
        textAppearance = ctx.resolveThemeAttribute(R.attr.textAppearanceListItem)
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

        add(handleImage, lParams {
            width = dp(24)
            height = dp(60)
            topOfParent()
            startOfParent(dp(18))
        })

        add(checkBox, lParams {
            width = dp(30)
            height = dp(60)
            topOfParent()
            after(handleImage, dp(12))
        })

        add(nameText, lParams {
            width = 0
            height = wrapContent
            bottomOfParent()
            before(editButton, dp(12))
            after(checkBox, dp(16))
            topOfParent()
        })

        add(editButton, lParams {
            width = dp(60)
            height = 0
            before(settingsButton)
            after(nameText)
            topOfParent()
            bottomOfParent()
        })

        add(settingsButton, lParams {
            width = dp(60)
            height = 0
            bottomOfParent()
            endOfParent()
            topOfParent()
        })
    }
}