package org.fcitx.fcitx5.android.input.bar

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.HorizontalScrollView
import android.widget.ViewAnimator
import android.widget.inline.InlineContentView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import kotlinx.coroutines.*
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.*
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.gravityVerticalCenter
import splitties.views.imageResource
import splitties.views.padding
import timber.log.Timber

sealed class KawaiiBarUi(override val ctx: Context, protected val theme: Theme) : Ui {

    companion object {
        val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation
    }

    protected fun toolButton(@DrawableRes icon: Int, initView: ToolButton.() -> Unit = {}) =
        ToolButton(ctx, icon, theme).apply(initView)

    class Candidate(ctx: Context, theme: Theme, private val horizontalView: View) :
        KawaiiBarUi(ctx, theme) {

        val expandButton = toolButton(R.drawable.ic_baseline_expand_more_24) {
            id = R.id.expand_candidate_btn
            visibility = View.INVISIBLE
        }

        override val root = ctx.constraintLayout {
            add(expandButton, lParams(dp(40)) {
                centerVertically()
                endOfParent()
            })
            add(horizontalView, lParams {
                centerVertically()
                startOfParent()
                before(expandButton)
            })
        }
    }

    class Idle(
        ctx: Context,
        theme: Theme,
        private val popupActionListener: PopupActionListener,
        private val popup: PopupComponent,
        private val commonKeyActionListener: CommonKeyActionListener
    ) : KawaiiBarUi(ctx, theme) {

        enum class State {
            Clipboard, Empty, NumberRow, InlineSuggestion
        }

        var currentState = State.Empty
            private set
        private val menuButtonRotation
            get() =
                when {
                    inPrivate -> 0f
                    isToolbarExpanded -> 90f
                    else -> -90f
                }

        private var inPrivate = false

        var isToolbarExpanded = false
            private set

        val menuButton = toolButton(R.drawable.ic_baseline_expand_more_24) {
            rotation = menuButtonRotation
        }

        val undoButton = toolButton(R.drawable.ic_baseline_undo_24)

        val redoButton = toolButton(R.drawable.ic_baseline_redo_24)

        val cursorMoveButton = toolButton(R.drawable.ic_cursor_move)

        val clipboardButton = toolButton(R.drawable.ic_clipboard)

        val moreButton = toolButton(R.drawable.ic_baseline_more_horiz_24)

        val hideKeyboardButton = toolButton(R.drawable.ic_baseline_arrow_drop_down_24)

        private fun ConstraintLayout.addButton(
            v: View,
            initParams: ConstraintLayout.LayoutParams.() -> Unit = {}
        ) {
            add(v, ConstraintLayout.LayoutParams(dp(40), dp(40)).apply {
                centerVertically()
                initParams(this)
            })
        }

        private val buttonsBar = constraintLayout {
            addButton(undoButton) { startOfParent(); before(redoButton) }
            addButton(redoButton) { after(undoButton); before(cursorMoveButton) }
            addButton(cursorMoveButton) { after(redoButton); before(clipboardButton) }
            addButton(clipboardButton) { after(cursorMoveButton); before(moreButton) }
            addButton(moreButton) { after(clipboardButton); endOfParent() }
        }

        private val clipboardIcon = imageView {
            imageResource = R.drawable.ic_clipboard
            colorFilter = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
        }

        private val clipboardText = textView {
            isSingleLine = true
            maxWidth = dp(120)
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(theme.altKeyTextColor)
        }

        private val clipboardSuggestionLayout = horizontalLayout {
            gravity = gravityCenter
            padding = dp(4)
            add(clipboardIcon, lParams(dp(20), dp(20)))
            add(clipboardText, lParams {
                leftMargin = dp(4)
            })
        }

        val clipboardSuggestionItem = object : CustomGestureView(ctx) {
            init {
                isHapticFeedbackEnabled = false
                background = rippleDrawable(theme.keyPressHighlightColor)
                add(clipboardSuggestionLayout, lParams(wrapContent, matchParent))
            }
        }

        private val clipboardBar = constraintLayout {
            add(clipboardSuggestionItem, lParams(wrapContent, matchConstraints) {
                centerInParent()
                verticalMargin = dp(4)
            })
        }

        private val emptyBar = constraintLayout()

        private val numberRowBar = object : BaseKeyboard(
            ctx,
            theme,
            listOf(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { digit ->
                KeyDef(
                    KeyDef.Appearance.Text(
                        displayText = digit,
                        textSize = 21f,
                        border = KeyDef.Appearance.Border.Off,
                        margin = false
                    ),
                    setOf(
                        KeyDef.Behavior.Press(KeyAction.SymAction(KeySym(digit.codePointAt(0))))
                    ),
                    arrayOf(KeyDef.Popup.Preview(digit))
                )
            })
        ) {}

        class InlineSuggestionsUi(override val ctx: Context) : Ui {

            private val scrollView = ctx.view(::HorizontalScrollView) {
                isFillViewport = true
                scrollBarSize = dp(1)
            }

            private val pinnedView = frameLayout()

            override val root = constraintLayout {
                add(scrollView, lParams(matchConstraints, matchParent) {
                    startOfParent()
                    before(pinnedView)
                    centerVertically()
                })
                add(pinnedView, lParams(wrapContent, matchParent) {
                    endOfParent()
                    centerVertically()
                })
            }

            fun clear() {
                scrollView.removeAllViews()
                pinnedView.removeAllViews()
            }

            @RequiresApi(Build.VERSION_CODES.R)
            fun setPinnedView(view: InlineContentView?) {
                pinnedView.removeAllViews()
                if (view != null) {
                    pinnedView.addView(view)
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            fun setScrollableViews(views: List<InlineContentView>) {
                val flexbox = view(::FlexboxLayout) {
                    flexWrap = FlexWrap.NOWRAP
                    justifyContent = JustifyContent.CENTER
                    views.forEach {
                        addView(it)
                        it.updateLayoutParams<FlexboxLayout.LayoutParams> {
                            flexShrink = 0f
                        }
                    }
                }
                scrollView.apply {
                    scrollTo(0, 0)
                    removeAllViews()
                    add(flexbox, lParams(wrapContent, matchParent))
                }
            }
        }

        val inlineSuggestionsBar = InlineSuggestionsUi(ctx)

        private val animator = ViewAnimator(ctx).apply {
            add(emptyBar, lParams(matchParent, matchParent))
            add(clipboardBar, lParams(matchParent, matchParent))
            add(buttonsBar, lParams(matchParent, matchParent))
            add(inlineSuggestionsBar.root, lParams(matchParent, matchParent))

            if (disableAnimation) {
                inAnimation = null
                outAnimation = null
            } else {
                inAnimation = AnimationSet(true).apply {
                    duration = 200L
                    addAnimation(AlphaAnimation(0f, 1f))
                    // TODO: rework InlineContentView animation
//                    addAnimation(ScaleAnimation(0f, 1f, 0f, 1f, 0f, dp(20f)))
                    addAnimation(TranslateAnimation(dp(-100f), 0f, 0f, 0f))
                }
                outAnimation = AnimationSet(true).apply {
                    duration = 200L
                    addAnimation(AlphaAnimation(1f, 0f))
//                    addAnimation(ScaleAnimation(1f, 0f, 1f, 0f, 0f, dp(20f)))
                    addAnimation(TranslateAnimation(0f, dp(-100f), 0f, 0f))
                }
            }
        }

        override val root = constraintLayout {
            addButton(menuButton) { startOfParent() }
            add(animator, lParams(matchConstraints, matchParent) {
                after(menuButton)
                before(hideKeyboardButton)
            })
            addButton(hideKeyboardButton) { endOfParent() }
            numberRowBar.visibility = View.GONE
            add(numberRowBar, lParams(matchParent, matchParent))
        }

        fun privateMode(activate: Boolean = true) {
            if (activate == inPrivate) return
            inPrivate = activate
            updateMenuButtonIcon()
            updateMenuButtonRotation(instant = true)
        }

        private fun updateMenuButtonIcon() {
            menuButton.image.imageResource =
                if (inPrivate) R.drawable.ic_view_private
                else R.drawable.ic_baseline_expand_more_24
        }

        private fun updateMenuButtonRotation(instant: Boolean = false) {
            val targetRotation = menuButtonRotation
            menuButton.apply {
                if (targetRotation == rotation) return
                if (instant || disableAnimation) {
                    rotation = targetRotation
                } else {
                    animate().setDuration(200L).rotation(targetRotation)
                }
            }
        }

        fun expandToolbar() {
            Timber.d("Expand idle ui toolbar at $currentState")
            if (animator.displayedChild != 2)
                animator.displayedChild = 2
            isToolbarExpanded = true
            updateMenuButtonRotation()
        }

        fun collapseToolbar() {
            Timber.d("Collapse idle ui toolbar at $currentState")
            switchUiByState(currentState)
            isToolbarExpanded = false
            updateMenuButtonRotation()
        }

        fun toggleToolbar() {
            if (isToolbarExpanded)
                collapseToolbar()
            else
                expandToolbar()
        }

        fun updateState(state: State) {
            currentState = state
            switchUiByState(state)
        }

        private fun switchUiByState(state: State) {
            Timber.d("Switch idle ui to $state")
            when (state) {
                State.Clipboard -> animator.displayedChild = 1
                State.Empty -> animator.displayedChild = 0
                State.NumberRow -> {}
                State.InlineSuggestion -> animator.displayedChild = 3
            }
            if (state != State.Empty) {
                isToolbarExpanded = false
            }
            if (state == State.NumberRow) {
                menuButton.visibility = View.GONE
                hideKeyboardButton.visibility = View.GONE
                animator.visibility = View.GONE
                numberRowBar.visibility = View.VISIBLE
                numberRowBar.keyActionListener = commonKeyActionListener.listener
                numberRowBar.popupActionListener = popupActionListener
            } else {
                menuButton.visibility = View.VISIBLE
                hideKeyboardButton.visibility = View.VISIBLE
                animator.visibility = View.VISIBLE
                numberRowBar.visibility = View.GONE
                numberRowBar.keyActionListener = null
                numberRowBar.popupActionListener = null
                popup.dismissAll()
            }
            updateMenuButtonRotation()
        }


        fun setClipboardItemText(text: String) {
            clipboardText.text = text
        }
    }

    class Title(ctx: Context, theme: Theme) : KawaiiBarUi(ctx, theme) {

        private val backButton = toolButton(R.drawable.ic_baseline_arrow_back_24)

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
            if (extension == null)
                return
            root.removeView(extension)
            extension = null
        }
    }

}