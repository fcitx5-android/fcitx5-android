package org.fcitx.fcitx5.android.input.popup

import android.content.Context
import android.view.ViewOutlineProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.gravityCenter

class PopupEntryUi(override val ctx: Context, val theme: Theme) : Ui {

    var lastShowTime = -1L

    val textView = textView {
        textSize = 28f
        gravity = gravityCenter
        setTextColor(theme.keyTextColor.color)
    }

    override val root = constraintLayout {
        backgroundColor = theme.keyBackgroundColor.color
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = dp(1f)
        add(textView, lParams(matchParent, dp(48)) {
            topOfParent()
            centerHorizontally()
        })
    }

    fun setText(text: String) {
        textView.text = text
    }
}
