package me.rocka.fcitx5test.input.bar

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.ViewFlipper
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.gravityVerticalCenter
import splitties.views.imageResource
import splitties.views.padding

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

    class Idle(ctx: Context, defaultMode: Mode = Mode.Clipboard) : KawaiiBarUi(ctx) {

        enum class Mode {
            Toolbar, Clipboard;

            val other: Mode
                get() = when (this) {
                    Clipboard -> Toolbar
                    Toolbar -> Clipboard
                }

            val flipperIndex: Int
                get() = when (this) {
                    Clipboard -> 0
                    Toolbar -> 1
                }

            val expandButtonRotation: Float
                get() = when (this) {
                    Clipboard -> -90f
                    Toolbar -> 90f
                }
        }

        override val state: KawaiiBarState
            get() = KawaiiBarState.Idle

        private var mode = defaultMode

        private var inPrivate = false

        private fun toolButton(@DrawableRes icon: Int) = imageButton {
            imageResource = icon
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val expandButton = toolButton(R.drawable.ic_baseline_expand_more_24).apply {
            rotation = mode.expandButtonRotation
        }

        val undoButton = toolButton(R.drawable.ic_baseline_undo_24)

        val redoButton = toolButton(R.drawable.ic_baseline_redo_24)

        val cursorMoveButton = toolButton(R.drawable.ic_cursor_move)

        val clipboardButton = toolButton(R.drawable.ic_clipboard)

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

        private val buttonsBar = constraintLayout {
            addButton(undoButton) { startOfParent(); before(redoButton) }
            addButton(redoButton) { after(undoButton); before(cursorMoveButton) }
            addButton(cursorMoveButton) { after(redoButton); before(clipboardButton) }
            addButton(clipboardButton) { after(cursorMoveButton); before(settingsButton) }
            addButton(settingsButton) { after(clipboardButton); endOfParent() }
        }

        val clipboardText = textView {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }

        val clipboardItem = horizontalLayout {
            visibility = View.INVISIBLE
            gravity = gravityCenter
            padding = dp(4)
            add(imageView {
                imageResource = R.drawable.ic_clipboard
            }, lParams(dp(20), dp(20)))
            add(clipboardText, lParams {
                leftMargin = dp(4)
            })
        }

        private val clipboardBar = constraintLayout {
            add(clipboardItem, lParams(wrapContent, matchConstraints) {
                topOfParent()
                startOfParent()
                endOfParent()
                bottomOfParent()
                horizontalMargin = dp(12)
            })
        }

        private val flipper = view(::ViewFlipper) {
            inAnimation = AnimationSet(true).apply {
                duration = 200L
                addAnimation(AlphaAnimation(0f, 1f))
                addAnimation(ScaleAnimation(0f, 1f, 0f, 1f, 0f, dp(20f)))
                addAnimation(TranslateAnimation(dp(-100f), 0f, 0f, 0f))
            }
            outAnimation = AnimationSet(true).apply {
                duration = 200L
                addAnimation(AlphaAnimation(1f, 0f))
                addAnimation(ScaleAnimation(1f, 0f, 1f, 0f, 0f, dp(20f)))
                addAnimation(TranslateAnimation(0f, dp(-100f), 0f, 0f))
            }
            add(clipboardBar, lParams(matchParent, matchParent))
            add(buttonsBar, lParams(matchParent, matchParent))
        }

        override val root = constraintLayout {
            addButton(expandButton) { startOfParent() }
            add(flipper, lParams(matchConstraints, dp(40)) {
                after(expandButton)
                before(hideKeyboardButton)
            })
            addButton(hideKeyboardButton) { endOfParent() }
        }

        fun privateMode(activate: Boolean = true) {
            inPrivate = activate
            expandButton.apply {
                when (inPrivate) {
                    true -> {
                        imageResource = R.drawable.ic_view_private
                        rotation = 0f
                    }
                    false -> {
                        imageResource = R.drawable.ic_baseline_expand_more_24
                        rotation = mode.expandButtonRotation
                    }
                }
            }
        }

        fun switchMode(to: Mode? = null) {
            val next = to ?: mode.other
            if (mode == next) return
            mode = next
            flipper.displayedChild = next.flipperIndex
            if (inPrivate) return
            expandButton.animate().setDuration(200L).rotation(next.expandButtonRotation)
        }

        fun updateClipboard(text: String) {
            if (text.isEmpty()) {
                clipboardItem.visibility = View.INVISIBLE
            } else {
                clipboardText.text = text
                clipboardItem.visibility = View.VISIBLE
                switchMode(Mode.Clipboard)
            }
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
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
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