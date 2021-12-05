package me.rocka.fcitx5test.ui.olist

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


class OrderedListEntryUi(override val ctx: Context) : Ui {

    val handleImage = imageView {
        imageResource = R.drawable.ic_baseline_drag_handle_24
        colorFilter = PorterDuffColorFilter(styledColor(R.attr.colorAccent), PorterDuff.Mode.SRC_IN)
    }

    val nameText = textView {
        minHeight = dp(60)
        setPaddingDp(0, 18, 0, 18)
        textAppearance = ctx.resolveThemeAttribute(R.attr.textAppearanceListItem)
    }

    val editButton = button {
        background = styledDrawable(android.R.attr.selectableItemBackground)
        setText(R.string.edit)
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
            height = dp(24)
            topOfParent(dp(18))
            startOfParent(dp(18))
        })

        add(nameText, lParams {
            width = 0
            height = wrapContent
            bottomOfParent()
            before(editButton, dp(12))
            after(handleImage, dp(16))
            topOfParent()
        })

        add(editButton, lParams {
            width = wrapContent
            height = wrapContent
            before(settingsButton, dp(12))
            after(nameText, dp(16))
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