package me.rocka.fcitx5test.input

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.Fcitx
import me.rocka.fcitx5test.core.FcitxEvent
import me.rocka.fcitx5test.input.candidates.CandidateViewBuilder
import me.rocka.fcitx5test.input.candidates.ExpandableCandidateComponent
import me.rocka.fcitx5test.input.candidates.HorizontalCandidateComponent
import me.rocka.fcitx5test.input.clipboard.ClipboardComponent
import me.rocka.fcitx5test.input.keyboard.BaseKeyboard
import me.rocka.fcitx5test.input.keyboard.KeyAction
import me.rocka.fcitx5test.input.keyboard.KeyboardComponent
import me.rocka.fcitx5test.input.preedit.PreeditContent
import me.rocka.fcitx5test.input.preedit.PreeditUi
import me.rocka.fcitx5test.utils.AppUtil
import me.rocka.fcitx5test.utils.dependency.wrapContext
import me.rocka.fcitx5test.utils.dependency.wrapFcitx
import me.rocka.fcitx5test.utils.dependency.wrapFcitxInputMethodService
import me.rocka.fcitx5test.utils.inputConnection
import org.mechdancer.dependency.plusAssign
import org.mechdancer.dependency.scope
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.systemservices.inputMethodManager
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.imageResource


@SuppressLint("ViewConstructor")
class InputView(
    val service: FcitxInputMethodService,
    val fcitx: Fcitx
) : ConstraintLayout(service) {

    private val themedContext = context.withTheme(R.style.Theme_FcitxAppTheme)

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
    private val keyboardManager = KeyboardComponent()

    private val candidateViewBuilder: CandidateViewBuilder = CandidateViewBuilder()

    private val horizontalCandidate = HorizontalCandidateComponent()
    private val expandableCandidate = ExpandableCandidateComponent {
        if (adapter.itemCount == 0) {
            shrink()
            expandCandidateButton.visibility = INVISIBLE
        } else {
            expandCandidateButton.visibility = VISIBLE
        }
    }

    private val scope = scope { }

    private val expandCandidateButton: ImageButton =
        themedContext.imageButton(R.id.expand_candidate_btn) {
            background = null
            imageResource = R.drawable.ic_baseline_expand_more_24
            setOnClickListener { expandableCandidate.expand() }
            visibility = INVISIBLE
        }

    private val keyActionListener = BaseKeyboard.KeyActionListener { action ->
        onAction(action)
    }

    private fun setupScope() {
        scope += wrapFcitxInputMethodService(service)
        scope += wrapContext(themedContext)
        scope += wrapFcitx(fcitx)
        scope += candidateViewBuilder
        scope += keyboardManager
        scope += expandableCandidate
        scope += horizontalCandidate
    }


    init {
        // MUST call before operation
        setupScope()

        service.lifecycleScope.launch {
            keyboardManager.updateCurrentIme(fcitx.currentIme())
        }
        preeditPopup.width = resources.displayMetrics.widthPixels
        backgroundColor = themedContext.styledColor(android.R.attr.colorBackground)
        add(expandCandidateButton, lParams(matchConstraints, dp(40)) {
            matchConstraintPercentWidth = 0.1f
            topOfParent()
            endOfParent()
        })
        add(keyboardManager.view, lParams(matchParent, wrapContent) {
            below(expandCandidateButton)
            startOfParent()
            endOfParent()
            bottomOfParent()
        })
        add(horizontalCandidate.view, lParams(matchConstraints, dp(40)) {
            topOfParent()
            startOfParent()
            before(expandCandidateButton)
        })
        add(expandableCandidate.view, lParams(matchConstraints, 0) {
            below(horizontalCandidate.view)
            startOfParent()
            endOfParent()
        })

        expandableCandidate.init()
        expandableCandidate.view.keyActionListener = keyActionListener
        expandableCandidate.onStateUpdate = {
            when (it) {
                ExpandableCandidateComponent.State.Expanded -> {
                    expandCandidateButton.setOnClickListener { expandableCandidate.shrink() }
                    expandCandidateButton.imageResource = R.drawable.ic_baseline_expand_less_24
                }
                ExpandableCandidateComponent.State.Shrunk -> {
                    expandCandidateButton.setOnClickListener { expandableCandidate.expand() }
                    expandCandidateButton.imageResource = R.drawable.ic_baseline_expand_more_24
                }
            }
        }

        keyboardManager.switchLayout("qwerty", keyActionListener)
    }

    override fun onDetachedFromWindow() {
        preeditPopup.dismiss()
        super.onDetachedFromWindow()
    }

    fun onShow() {
        keyboardManager.showKeyboard()
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
                keyboardManager.updateCurrentIme(it.data.status)
            }
            else -> {
            }
        }
    }

    private fun onAction(action: KeyAction<*>) {
        service.lifecycleScope.launch {
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
                is KeyAction.LayoutSwitchAction -> keyboardManager.switchLayout(
                    action.act,
                    keyActionListener
                )
                is KeyAction.CustomAction -> customEvent(action.act)
                else -> {
                }
            }
        }
    }

    fun updatePreedit(content: PreeditContent) {
        keyboardManager.updatePreedit(content)
        expandableCandidate.view.onPreeditChange(null, content)
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
        expandableCandidate.view.resetPosition()
    }

    private suspend fun quickPhrase() {
        fcitx.reset()
        fcitx.triggerQuickPhrase()
    }

    private suspend fun unicode() {
        fcitx.triggerUnicode()
    }

    private suspend fun switchLang() {
        if (fcitx.enabledIme().size < 2) {
            AppUtil.launchMainToAddInputMethods(context)
        } else
            fcitx.enumerateIme()
    }

    private inline fun customEvent(fn: (Fcitx) -> Unit) {
        fn(fcitx)
    }

}
