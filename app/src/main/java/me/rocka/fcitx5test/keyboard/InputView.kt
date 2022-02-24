package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.view.Gravity
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

    private val candidateViewAdapter = object : CandidateViewAdapter() {
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
            SpanHelper(candidateViewAdapter, this).attach()
        }
        adapter = candidateViewAdapter

        GridDecoration(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.candidate_divider,
                context.theme
            )!!
        ).also { addItemDecoration(it) }
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
        candidateViewAdapter.updateCandidates(data)
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
