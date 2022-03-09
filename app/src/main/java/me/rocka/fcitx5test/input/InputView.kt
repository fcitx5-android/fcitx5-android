package me.rocka.fcitx5test.input

import android.annotation.SuppressLint
import android.widget.ImageButton
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
import me.rocka.fcitx5test.input.keyboard.KeyboardComponent
import me.rocka.fcitx5test.input.preedit.PreeditComponent
import me.rocka.fcitx5test.utils.dependency.wrapContext
import me.rocka.fcitx5test.utils.dependency.wrapFcitx
import me.rocka.fcitx5test.utils.dependency.wrapFcitxInputMethodService
import me.rocka.fcitx5test.utils.dependency.wrapInputView
import org.mechdancer.dependency.plusAssign
import org.mechdancer.dependency.scope
import splitties.dimensions.dp
import splitties.resources.styledColor
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

    private val broadcaster = InputBroadcaster()

    private val preedit = PreeditComponent()

    private val keyboard = KeyboardComponent()

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

    private fun setupScope() {
        scope += wrapFcitxInputMethodService(service)
        scope += wrapContext(themedContext)
        scope += wrapFcitx(fcitx)
        scope += candidateViewBuilder
        scope += keyboard
        scope += expandableCandidate
        scope += horizontalCandidate
        scope += preedit
        scope += broadcaster
        scope += wrapInputView(this)
    }


    init {
        // MUST call before any operation
        setupScope()

        service.lifecycleScope.launch {
            broadcaster.broadcastImeUpdate(fcitx.currentIme())
        }
        backgroundColor = themedContext.styledColor(android.R.attr.colorBackground)
        add(expandCandidateButton, lParams(matchConstraints, dp(40)) {
            matchConstraintPercentWidth = 0.1f
            topOfParent()
            endOfParent()
        })
        add(keyboard.view, lParams(matchParent, wrapContent) {
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
        expandableCandidate.view.keyActionListener = keyboard.listener
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

        keyboard.switchLayout("qwerty")
    }

    override fun onDetachedFromWindow() {
        preedit.dismiss()
        super.onDetachedFromWindow()
    }

    fun onShow() {
        keyboard.showKeyboard()
    }

    fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                broadcaster.broadcastCandidatesUpdate(it.data)
            }
            is FcitxEvent.PreeditEvent -> {
                preedit.updatePreedit(it)
            }
            is FcitxEvent.InputPanelAuxEvent -> {
                preedit.updatePreedit(it)
            }
            is FcitxEvent.IMChangeEvent -> {
                broadcaster.broadcastImeUpdate(it.data.status)
            }
            else -> {
            }
        }
    }

}
