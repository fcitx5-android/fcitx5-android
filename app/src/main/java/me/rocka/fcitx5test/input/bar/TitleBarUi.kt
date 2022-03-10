package me.rocka.fcitx5test.input.bar

import android.content.Context
import android.view.View
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource

class TitleBarUi(override val ctx: Context) : Ui {

    val backButton = imageButton {
        imageResource = R.drawable.ic_baseline_arrow_back_24
        background = null
    }

    val titleText = textView {
        textSize = 16f
    }

    var extension: View? = null

    override val root = constraintLayout {
        add(backButton, lParams(dp(40), dp(40)) {
            topOfParent()
            startOfParent()
            bottomOfParent()
        })
        add(titleText, lParams(wrapContent, dp(40)) {
            topOfParent()
            after(backButton, dp(20))
            bottomOfParent()
        })
    }

    fun addExtension(view: View) {
        if (extension != null) {
            throw Error("TitleBar extension already present")
        }
        extension = view
        root.run {
            add(view, lParams(wrapContent, dp(40)) {
                topOfParent()
                endOfParent()
                bottomOfParent()
            })
        }
    }

    fun removeExtension() {
        if (extension == null) {
            throw Error("TitleBar extension is empty")
        }
        root.removeView(extension)
        extension = null
    }
}