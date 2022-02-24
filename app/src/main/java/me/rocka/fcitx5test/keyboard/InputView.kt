package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.Fcitx
import me.rocka.fcitx5test.core.FcitxEvent
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.keyboard.layout.BaseKeyboard
import me.rocka.fcitx5test.keyboard.layout.KeyAction
import me.rocka.fcitx5test.keyboard.layout.NumberKeyboard
import me.rocka.fcitx5test.keyboard.layout.TextKeyboard
import me.rocka.fcitx5test.utils.globalLayoutListener
import me.rocka.fcitx5test.utils.inputConnection
import me.rocka.fcitx5test.utils.oneShotGlobalLayoutListener
import splitties.dimensions.dp
import splitties.resources.dimenPxSize
import splitties.resources.str
import splitties.resources.styledColor
import splitties.systemservices.inputMethodManager
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource


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


    @Suppress("PrivatePropertyName")
    private val Commons = object {
        fun newCandidateViewAdapter() = object : CandidateViewAdapter() {
            override fun onTouchDown() = currentKeyboard.haptic()
            override fun onSelect(idx: Int) {
                service.lifecycleScope.launch { fcitx.select(idx) }
            }
        }

        // setup a listener that sets the span count of gird layout according to recycler view's width
        fun RecyclerView.autoSpanCount() {
            oneShotGlobalLayoutListener {
                (layoutManager as GridLayoutManager).apply {
                    // set columns according to the width of recycler view
                    // last item doesn't need padding, so we assume recycler view is wider
                    spanCount = (measuredWidth + dimenPxSize(R.dimen.candidate_padding)) /
                            (dimenPxSize(R.dimen.candidate_min_width) + dimenPxSize(R.dimen.candidate_padding))
                    requestLayout()
                }
            }
        }

        fun RecyclerView.addGridDecoration() = GridDecoration(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.candidate_divider,
                context.theme
            )!!
        ).also { addItemDecoration(it) }

        fun RecyclerView.setupGridLayoutManager(
            adapter: CandidateViewAdapter,
            scrollVertically: Boolean
        ) {
            layoutManager =
                object : GridLayoutManager(
                    themedContext, INITIAL_SPAN_COUNT, VERTICAL, false
                ) {
                    override fun canScrollVertically(): Boolean {
                        return scrollVertically
                    }
                }.apply {
                    SpanHelper(adapter, this).attach()
                }
            this.adapter = adapter
        }

    }


    inner class ExpandedCandidate {
        val adapter = Commons.newCandidateViewAdapter()
        val ui = ExpandedCandidateUi(themedContext) {
            with(Commons) {
                autoSpanCount()
                setupGridLayoutManager(this@ExpandedCandidate.adapter, true)
                addGridDecoration()
            }
        }
    }

    inner class HorizontalCandidate {
        val adapter = Commons.newCandidateViewAdapter()
        val recyclerView = themedContext.recyclerView(R.id.candidate_view) {
            isVerticalScrollBarEnabled = false
            backgroundColor = styledColor(android.R.attr.colorBackground)
            with(Commons) {
                autoSpanCount()
                setupGridLayoutManager(this@HorizontalCandidate.adapter, false)
                addGridDecoration()
            }
            globalLayoutListener {
                expandedCandidate.adapter.offset = childCount
            }
        }
    }

    private val horizontalCandidate = HorizontalCandidate()
    private val expandedCandidate = ExpandedCandidate()

    private var candidateViewExpanded = false
    private val expandCandidateButton = themedContext.imageButton(R.id.expand_candidate_btn) {
        elevation = dp(2f)
        imageResource = R.drawable.ic_baseline_expand_more_24
        setOnClickListener {
            candidateViewExpanded = !candidateViewExpanded
            val btn = it as ImageButton
            val lp = expandedCandidate.ui.root.layoutParams as LayoutParams
            if (candidateViewExpanded) {
                btn.imageResource = R.drawable.ic_baseline_expand_less_24
                lp.bottomToBottom = LayoutParams.PARENT_ID
                lp.height = matchConstraints
            } else {
                btn.imageResource = R.drawable.ic_baseline_expand_more_24
                lp.bottomToBottom = LayoutParams.UNSET
                lp.height = 0
                expandedCandidate.ui.resetPosition()
            }
            expandedCandidate.ui.root.requestLayout()
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
        preeditPopup.width = resources.displayMetrics.widthPixels
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
        add(horizontalCandidate.recyclerView, lParams(matchConstraints, dp(40)) {
            topOfParent()
            startOfParent()
            before(expandCandidateButton)
        })
        add(expandedCandidate.ui.root, lParams(matchConstraints, 0) {
            below(horizontalCandidate.recyclerView)
            startOfParent()
            endOfParent()
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
        horizontalCandidate.adapter.updateCandidates(data)
        expandedCandidate.adapter.updateCandidates(data)
        expandedCandidate.ui.resetPosition()
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

    companion object {
        private const val INITIAL_SPAN_COUNT = 6
    }
}
