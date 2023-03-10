package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.graphics.Typeface
import android.view.View
import androidx.core.view.isVisible
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter

class TitleUi(override val ctx: Context, theme: Theme) : Ui {

    private val backButton = ToolButton(ctx, R.drawable.ic_baseline_arrow_back_24, theme).apply {
        id = R.id.expand_candidate_btn
    }

    private val titleText = textView {
        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        setTextColor(theme.altKeyTextColor)
        gravity = gravityVerticalCenter
        textSize = 16f
    }

    private var extension: View? = null

    override val root = constraintLayout {
        add(backButton, lParams(dp(40), dp(40)) {
            topOfParent()
            startOfParent()
            bottomOfParent()
        })
        add(titleText, lParams(wrapContent, dp(40)) {
            topOfParent()
            after(backButton, dp(8))
            bottomOfParent()
        })
    }

    fun setReturnButtonOnClickListener(block: () -> Unit) {
        backButton.setOnClickListener {
            block()
        }
    }

    fun setTitle(title: String) {
        titleText.text = title
    }

    fun addExtension(view: View, showTitle: Boolean) {
        if (extension != null) {
            throw IllegalStateException("TitleBar extension is already present")
        }
        backButton.isVisible = showTitle
        titleText.isVisible = showTitle
        extension = view
        root.run {
            add(view, lParams(matchConstraints, dp(40)) {
                centerVertically()
                if (showTitle) {
                    endOfParent(dp(5))
                } else {
                    centerHorizontally()
                }
            })
        }
    }

    fun removeExtension() {
        extension?.let {
            root.removeView(it)
            extension = null
        }
    }
}
