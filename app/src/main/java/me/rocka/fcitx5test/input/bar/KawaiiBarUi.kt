package me.rocka.fcitx5test.input.bar

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource

sealed class KawaiiBarUi(override val ctx: Context) : Ui {

    class Candidate(ctx: Context, private val horizontalView: View) : KawaiiBarUi(ctx) {

        val expandButton = imageButton(R.id.expand_candidate_btn) {
            background = null
            imageResource = R.drawable.ic_baseline_expand_more_24
            visibility = ConstraintLayout.INVISIBLE
        }

        override val root = ctx.constraintLayout {
            add(expandButton, lParams(matchConstraints, dp(40)) {
                matchConstraintPercentWidth = 0.1f
                topOfParent()
                endOfParent()
                bottomOfParent()
            })
            add(horizontalView, lParams(matchConstraints, dp(40)) {
                topOfParent()
                startOfParent()
                before(expandButton)
                bottomOfParent()
            })
        }
    }

    class Idle(ctx: Context) : KawaiiBarUi(ctx) {

        val expandMenuButton = imageButton {
            background = null
            imageResource = R.drawable.ic_baseline_expand_more_24
            rotation = -90f
        }

        val hideKeyboardButton = imageButton {
            background = null
            imageResource = R.drawable.ic_baseline_arrow_drop_down_24
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

    class Title(ctx: Context) : KawaiiBarUi(ctx) {

        private val backButton = imageButton {
            imageResource = R.drawable.ic_baseline_arrow_back_24
            background = null
        }

        private val titleText = textView {
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
                after(backButton, dp(20))
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

        fun addExtension(view: View) {
            if (extension != null) {
                throw IllegalStateException("TitleBar extension is already present")
            }
            extension = view
            root.run {
                add(view, lParams(wrapContent, dp(40)) {
                    topOfParent()
                    endOfParent(dp(5))
                    bottomOfParent()
                })
            }
        }

        fun removeExtension() {
            if (extension == null)
                return
            root.removeView(extension)
            extension = null
        }
    }

}