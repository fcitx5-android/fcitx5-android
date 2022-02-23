package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.Fcitx
import me.rocka.fcitx5test.core.FcitxEvent
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.keyboard.layout.BaseKeyboard
import me.rocka.fcitx5test.keyboard.layout.KeyAction
import me.rocka.fcitx5test.keyboard.layout.NumberKeyboard
import me.rocka.fcitx5test.keyboard.layout.TextKeyboard
import me.rocka.fcitx5test.utils.inputConnection
import splitties.dimensions.dp
import splitties.resources.dimenPxSize
import splitties.resources.str
import splitties.resources.styledColor
import splitties.systemservices.inputMethodManager
import splitties.systemservices.windowManager
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource
import kotlin.math.ceil
import kotlin.math.min


@SuppressLint("ViewConstructor")
class InputView(
    val service: FcitxInputMethodService,
    val fcitx: Fcitx
) : ConstraintLayout(service) {

    data class PreeditContent(
        var preedit: FcitxEvent.PreeditEvent.Data,
        var aux: FcitxEvent.InputPanelAuxEvent.Data
    )

    private val themedContext = context.withTheme(R.style.Theme_AppCompat_DayNight)

    private var currentIme = InputMethodEntry(service.str(R.string._not_available_))
    private val cachedPreedit = PreeditContent(
        FcitxEvent.PreeditEvent.Data("", "", 0),
        FcitxEvent.InputPanelAuxEvent.Data("", "")
    )
    private val preeditUi = PreeditUi(themedContext)
    private val preeditPopup = PopupWindow(
        preeditUi.root,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    ).apply {
        isTouchable = false
        isClippingEnabled = false
    }

    private val candidateViewAdp = object : CandidateViewAdapter() {
        override fun onTouchDown() = currentKeyboard.haptic()
        override fun onSelect(idx: Int) {
            service.lifecycleScope.launch { fcitx.select(idx) }
        }
    }
    private var candidateViewExpanded = false
    private val candidateView = themedContext.recyclerView(R.id.candidate_view) {
        isVerticalScrollBarEnabled = false
        backgroundColor = styledColor(android.R.attr.colorBackground)
        var listener: ViewTreeObserver.OnGlobalLayoutListener? = null
        listener = ViewTreeObserver.OnGlobalLayoutListener {
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
            (layoutManager as GridLayoutManager).apply {
                // set columns according to the width of recycler view
                spanCount =
                        // last item doesn't need padding, so we assume recycler view is wider
                    ((measuredWidth + dimenPxSize(R.dimen.candidate_padding)) / (dimenPxSize(R.dimen.candidate_min_width)
                            + dimenPxSize(R.dimen.candidate_padding)))
                requestLayout()
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        // initially 6 columns
        layoutManager = object : GridLayoutManager(themedContext, 6, VERTICAL, false) {
            override fun canScrollVertically() =
                if (!candidateViewExpanded) false else super.canScrollVertically()
        }.apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                // enable cache so that we can to some extent get the rid of the nondeterminism
                init {
                    isSpanIndexCacheEnabled = true
                    isSpanGroupIndexCacheEnabled = true
                }

                private var occupiedSpan = 0

                // two words per span
                private fun getMinSpanSize(position: Int) = min(
                    ceil(candidateViewAdp.measureWidth(position) / 2).toInt(), spanCount
                )

                override fun getSpanSize(position: Int): Int {
                    return getMinSpanSize(position)
                    // TODO
//                    if (occupiedSpan >= spanCount)
//                        occupiedSpan = 0
//
//                    val currentSpanSize = getMinSpanSize(position)
//
//                    // this implementation seems flaky and nondeterministic
//                    if (position + 1 < candidateViewAdp.candidates.size) {
//                        val nextSpan = getMinSpanSize(position + 1)
//                        // current row still can accommodate next item
//                        return if (spanCount - (occupiedSpan + currentSpanSize) >= nextSpan) {
//                            occupiedSpan += currentSpanSize
//                            currentSpanSize
//                        } else {
//                            // space in current row can't accommodate next item
//                            // so we fill current row by current item
//                            val newCurrentSpanSize = max(spanCount - occupiedSpan, currentSpanSize)
//                            occupiedSpan += newCurrentSpanSize
//                            newCurrentSpanSize
//                        }
//                    } else {
//                        occupiedSpan += currentSpanSize
//                        return currentSpanSize
//                    }
                }

            }
        }
        adapter = candidateViewAdp

        object : ItemDecoration() {
            val drawable =
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.candidate_divider,
                    context.theme
                )!!

            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val lp = view.layoutParams as GridLayoutManager.LayoutParams
                val layoutManager = parent.layoutManager as GridLayoutManager
                // add space for the last item in each row
                if (lp.spanIndex + lp.spanSize != layoutManager.spanCount) {
                    outRect.right = drawable.intrinsicWidth
                } else {
                    outRect.set(0, 0, 0, 0)
                }
                // always add bottom padding
                outRect.bottom = dp(10)
            }

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val layoutManager = parent.layoutManager as GridLayoutManager
                for (i in 0 until layoutManager.childCount) {
                    val view = parent.getChildAt(i)
                    val lp = view.layoutParams as GridLayoutManager.LayoutParams
                    // draw divider if it is not the last item in each row
                    if (lp.spanIndex + lp.spanSize == layoutManager.spanCount)
                        continue
                    val left = view.right + lp.rightMargin
                    val right = left + drawable.intrinsicWidth
                    val top = view.top - lp.topMargin
                    val bottom = view.bottom + lp.bottomMargin
                    // make the divider shorter
                    drawable.setBounds(left, top + dp(6), right, bottom - dp(6))
                    drawable.draw(c)
                }
            }

        }.also { addItemDecoration(it) }
        PagerSnapHelper().attachToRecyclerView(this)
    }
    private val expandCandidateButton = themedContext.imageButton(R.id.expand_candidate_btn) {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_expand_more_24
        setOnClickListener {
            candidateViewExpanded = !candidateViewExpanded
            val btn = it as ImageButton
            val lp = candidateView.layoutParams as LayoutParams
            if (candidateViewExpanded) {
                btn.imageResource = R.drawable.ic_baseline_expand_less_24
                lp.bottomToBottom = LayoutParams.PARENT_ID
                lp.height = matchConstraints
            } else {
                btn.imageResource = R.drawable.ic_baseline_expand_more_24
                lp.bottomToBottom = LayoutParams.UNSET
                lp.height = dp(40)
                candidateView.scrollToPosition(0)
            }
            candidateView.requestLayout()
        }
    }
    private val keyboardView = themedContext.frameLayout(R.id.keyboard_view)

    private val keyboards: HashMap<String, BaseKeyboard> = hashMapOf(
        "qwerty" to TextKeyboard(themedContext),
        "number" to NumberKeyboard(themedContext)
    )
    private var currentKeyboardName = ""
    private val currentKeyboard: BaseKeyboard get() = keyboards.getValue(currentKeyboardName)

    private val keyActionListener = BaseKeyboard.KeyActionListener { action ->
        onAction(action)
    }

    init {
        service.lifecycleScope.launch {
            currentIme = fcitx.currentIme()
            currentKeyboard.onInputMethodChange(currentIme)
        }
        preeditPopup.width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            DisplayMetrics().let {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(it)
                it.widthPixels
            }
        }
        backgroundColor = themedContext.styledColor(android.R.attr.colorBackground)
        add(expandCandidateButton, lParams(matchConstraints, dp(40)) {
            matchConstraintPercentWidth = 0.1f
            topOfParent()
            endOfParent()
        })
        add(keyboardView, lParams(matchParent, wrapContent) {
            below(expandCandidateButton)
            startOfParent()
            endOfParent()
            bottomOfParent()
        })
        add(candidateView, lParams(matchConstraints, dp(40)) {
            topOfParent()
            startOfParent()
            before(expandCandidateButton)
        })
        switchLayout("qwerty")
    }

    override fun onDetachedFromWindow() {
        preeditPopup.dismiss()
        super.onDetachedFromWindow()
    }

    fun onShow() {
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                updateCandidates(it.data)
            }
            is FcitxEvent.PreeditEvent -> it.data.let {
                cachedPreedit.preedit = it
                updatePreedit(cachedPreedit)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                cachedPreedit.aux = it.data
                updatePreedit(cachedPreedit)
            }
            is FcitxEvent.IMChangeEvent -> {
                currentIme = it.data.status
                currentKeyboard.onInputMethodChange(currentIme)
            }
            else -> {
            }
        }
    }

    private fun onAction(action: KeyAction<*>) = service.lifecycleScope.launch {
        when (action) {
            is KeyAction.FcitxKeyAction -> fcitx.sendKey(action.act)
            is KeyAction.CommitAction -> {
                // TODO: this should be handled more gracefully; or CommitAction should be removed?
                fcitx.reset()
                service.inputConnection?.commitText(action.act, 1)
            }
            is KeyAction.RepeatStartAction -> service.startRepeating(action.act)
            is KeyAction.RepeatEndAction -> service.cancelRepeating(action.act)
            is KeyAction.QuickPhraseAction -> quickPhrase()
            is KeyAction.UnicodeAction -> unicode()
            is KeyAction.LangSwitchAction -> switchLang()
            is KeyAction.InputMethodSwitchAction -> inputMethodManager.showInputMethodPicker()
            is KeyAction.LayoutSwitchAction -> switchLayout(action.act)
            is KeyAction.CustomAction -> customEvent(action.act)
            else -> {
            }
        }
    }

    fun updatePreedit(content: PreeditContent) {
        currentKeyboard.onPreeditChange(service.editorInfo, content)
        preeditUi.update(content)
        preeditPopup.run {
            if (!preeditUi.visible) {
                dismiss()
                return
            }
            val height = preeditUi.measureHeight(width)
            if (isShowing) {
                update(
                    0, -height,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            } else {
                showAtLocation(this@InputView, Gravity.NO_GRAVITY, 0, -height)
            }
        }
    }

    private fun updateCandidates(data: Array<String>) {
        candidateViewAdp.candidates = data
        candidateView.scrollToPosition(0)
    }

    private fun switchLayout(to: String) {
        keyboards[currentKeyboardName]?.let {
            it.keyActionListener = null
            it.onDetach()
            keyboardView.removeView(it)
        }
        currentKeyboardName = to.ifEmpty {
            when (currentKeyboardName) {
                "qwerty" -> "number"
                else -> "qwerty"
            }
        }
        keyboardView.add(currentKeyboard, FrameLayout.LayoutParams(matchParent, wrapContent))
        currentKeyboard.keyActionListener = keyActionListener
        currentKeyboard.onAttach(service.editorInfo)
        currentKeyboard.onInputMethodChange(currentIme)
    }

    private suspend fun quickPhrase() {
        fcitx.reset()
        fcitx.triggerQuickPhrase()
    }

    private suspend fun unicode() {
        fcitx.triggerUnicode()
    }

    private suspend fun switchLang() {
        fcitx.enumerateIme()
    }

    private inline fun customEvent(fn: (Fcitx) -> Unit) {
        fn(fcitx)
    }
}
