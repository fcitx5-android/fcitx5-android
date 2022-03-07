package me.rocka.fcitx5test.keyboard

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
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
import me.rocka.fcitx5test.keyboard.candidates.CandidateViewBuilder
import me.rocka.fcitx5test.keyboard.candidates.ExpandableCandidate
import me.rocka.fcitx5test.keyboard.candidates.HorizontalCandidate
import me.rocka.fcitx5test.keyboard.layout.BaseKeyboard
import me.rocka.fcitx5test.keyboard.layout.KeyAction
import me.rocka.fcitx5test.ui.main.MainActivity
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
    private val keyboardManager = KeyboardManager()

    private val candidateViewBuilder: CandidateViewBuilder = CandidateViewBuilder()

    private val horizontalCandidate = HorizontalCandidate()
    private val expandableCandidate = ExpandableCandidate {
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
        add(keyboardManager.keyboardView, lParams(matchParent, wrapContent) {
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
        add(expandableCandidate.layout, lParams(matchConstraints, 0) {
            below(horizontalCandidate.recyclerView)
            startOfParent()
            endOfParent()
        })

        expandableCandidate.init()
        expandableCandidate.layout.keyActionListener = keyActionListener
        expandableCandidate.onStateUpdate = {
            when (it) {
                ExpandableCandidate.State.Expanded -> {
                    expandCandidateButton.setOnClickListener { expandableCandidate.shrink() }
                    expandCandidateButton.imageResource = R.drawable.ic_baseline_expand_less_24
                }
                ExpandableCandidate.State.Shrunk -> {
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
                is KeyAction.StartSettingsActivityAction -> context.startActivity(
                    Intent(
                        context,
                        MainActivity::class.java
                    ).apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
                )
                is KeyAction.CustomAction -> customEvent(action.act)
                else -> {
                }
            }
        }
    }

    fun updatePreedit(content: PreeditContent) {
        keyboardManager.updatePreedit(content)
        expandableCandidate.layout.onPreeditChange(null, content)
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
        expandableCandidate.layout.resetPosition()
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
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.INTENT_DATA_ADD_IM, 0)
            })
        } else
            fcitx.enumerateIme()
    }

    private inline fun customEvent(fn: (Fcitx) -> Unit) {
        fn(fcitx)
    }

}
