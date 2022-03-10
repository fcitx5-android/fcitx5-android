package me.rocka.fcitx5test.input.bar

import android.content.Context
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageButton
import splitties.views.imageResource

class IdleBarUi(override val ctx: Context): Ui {

    val expandMenuButton = imageButton {
        background = null
        imageResource = R.drawable.ic_baseline_expand_more_24
        rotation = -90f
    }

    val hideKeyboardButton = imageButton {
        background = null
        imageResource  = R.drawable.ic_baseline_arrow_drop_down_24
    }

    override val root = ctx.constraintLayout {
        add(expandMenuButton, lParams(dp(40), dp(40)) {
            topOfParent()
            startOfParent()
            bottomOfParent()
        })
        add(hideKeyboardButton, lParams(dp(40), dp(40)) {
            topOfParent()
            endOfParent()
            bottomOfParent()
        })
    }
}