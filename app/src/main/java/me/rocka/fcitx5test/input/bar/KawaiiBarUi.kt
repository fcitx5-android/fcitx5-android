package me.rocka.fcitx5test.input.bar

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityVerticalCenter
import splitties.views.imageResource

sealed class KawaiiBarUi(override val ctx: Context) : Ui {

    abstract val state: KawaiiBarState

    class Candidate(ctx: Context, private val horizontalView: View) : KawaiiBarUi(ctx) {

        override val state: KawaiiBarState
            get() = KawaiiBarState.Candidate

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

        val privateModeIcon = imageView {
            imageResource = R.drawable.ic_view_private
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        override val state: KawaiiBarState
            get() = KawaiiBarState.Idle

        private fun toolButton(@DrawableRes icon: Int) = imageButton {
            imageResource = icon
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val undoButton = toolButton(R.drawable.ic_baseline_undo_24)

        val redoButton = toolButton(R.drawable.ic_baseline_redo_24)

        val pasteButton = toolButton(R.drawable.ic_baseline_content_paste_24)

        val clipboardButton = toolButton(R.drawable.ic_outline_assignment_24)

        val settingsButton = toolButton(R.drawable.ic_baseline_settings_24)

        val hideKeyboardButton = imageButton {
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            imageResource = R.drawable.ic_baseline_arrow_drop_down_24
        }

        private fun ConstraintLayout.addButton(
            v: View,
            initParams: ConstraintLayout.LayoutParams.() -> Unit = {}
        ) {
            add(v, ConstraintLayout.LayoutParams(dp(40), dp(40)).apply {
                topOfParent()
                bottomOfParent()
                initParams(this)
            })
        }

        override val root = ctx.constraintLayout {
            addButton(privateModeIcon) { startOfParent() }
            addButton(undoButton) { after(privateModeIcon); before(redoButton) }
            addButton(redoButton) { after(undoButton); before(pasteButton) }
            addButton(pasteButton) { after(redoButton); before(clipboardButton) }
            addButton(clipboardButton) { after(pasteButton); before(settingsButton) }
            addButton(settingsButton) { after(clipboardButton); before(hideKeyboardButton) }
            addButton(hideKeyboardButton) { endOfParent() }
        }

        fun privateMode(activate: Boolean = true) {
            privateModeIcon.visibility = if (activate) View.VISIBLE else View.INVISIBLE
        }
    }

    class Title(ctx: Context) : KawaiiBarUi(ctx) {

        override val state: KawaiiBarState
            get() = KawaiiBarState.Title

        private val backButton = imageButton {
            imageResource = R.drawable.ic_baseline_arrow_back_24
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        private val titleText = textView {
            setTypeface(null, Typeface.BOLD)
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