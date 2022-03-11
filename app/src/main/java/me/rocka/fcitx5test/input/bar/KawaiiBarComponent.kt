package me.rocka.fcitx5test.input.bar

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.bar.KawaiiBarState.*
import me.rocka.fcitx5test.input.bar.KawaiiBarTransitionEvent.*
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.candidates.ExpandedCandidateWindow
import me.rocka.fcitx5test.input.candidates.HorizontalCandidateComponent
import me.rocka.fcitx5test.input.clipboard.ClipboardWindow
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.dependency.inputMethodService
import me.rocka.fcitx5test.input.wm.InputWindow
import me.rocka.fcitx5test.input.wm.InputWindowManager
import me.rocka.fcitx5test.utils.AppUtil
import me.rocka.fcitx5test.utils.EventStateMachine
import me.rocka.fcitx5test.utils.inputConnection
import me.rocka.fcitx5test.utils.on
import me.rocka.fcitx5test.utils.transitTo
import org.mechdancer.dependency.Component
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import org.mechdancer.dependency.plusAssign
import splitties.bitflags.hasFlag
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()
    private val windowManager: InputWindowManager by manager.must()
    private val service by manager.inputMethodService()

    private lateinit var scope: DynamicScope

    private val horizontalCandidate: HorizontalCandidateComponent by lazyComponent {
        HorizontalCandidateComponent()
    }

    private val idleUi by lazy {
        KawaiiBarUi.Idle(context).also {
            it.undoButton.setOnClickListener {
                service.sendCombinationKeyEvents(KeyEvent.KEYCODE_Z, ctrl = true)
            }
            it.redoButton.setOnClickListener {
                service.sendCombinationKeyEvents(KeyEvent.KEYCODE_Z, ctrl = true, shift = true)
            }
            it.pasteButton.setOnClickListener {
                service.inputConnection?.performContextMenuAction(android.R.id.paste)
            }
            it.clipboardButton.setOnClickListener {
                windowManager.attachWindow(ClipboardWindow())
            }
            it.settingsButton.setOnClickListener {
                AppUtil.launchMain(context)
            }
            it.hideKeyboardButton.setOnClickListener {
                service.requestHideSelf(0)
            }
        }
    }

    private val candidateUi by lazy {
        KawaiiBarUi.Candidate(context, horizontalCandidate.view)
    }

    private val titleUi by lazy { KawaiiBarUi.Title(context) }

    lateinit var currentUi: KawaiiBarUi
        private set

    private val stateMachine: EventStateMachine<KawaiiBarState, KawaiiBarTransitionEvent> =
        EventStateMachine(
            Idle,
            // from idle
            Idle on CandidatesUpdatedEmpty transitTo Idle,
            Idle on SimpleWindowAttached transitTo Idle,
            Idle on ExtendedWindowAttached transitTo Title,
            Idle on CandidatesUpdatedNonEmpty transitTo Candidate,
            // from title
            Title on WindowDetached transitTo Idle,
            // from candidate
            Candidate on CandidatesUpdatedNonEmpty transitTo Candidate,
            Candidate on CandidatesUpdatedEmpty transitTo Idle
        ).apply {
            onNewStateListener = {
                when (it) {
                    Idle -> switchUi(idleUi)
                    Candidate -> switchUi(candidateUi)
                    Title -> switchUi(titleUi)
                }
            }
        }

    private fun prepareAnimation() {
        val transition = AutoTransition()
        transition.duration = 50
        TransitionManager.beginDelayedTransition(view, transition)
    }


    // set expand candidate button to create expand candidate
    fun setExpandButtonToAttach() {
        candidateUi.expandButton.setOnClickListener {
            windowManager.attachWindow(ExpandedCandidateWindow())
        }
        candidateUi.expandButton.imageResource =
            R.drawable.ic_baseline_expand_more_24
    }

    // set expand candidate button to close expand candidate
    fun setExpandButtonToDetach() {
        candidateUi.expandButton.setOnClickListener {
            windowManager.switchToKeyboardWindow()
        }
        candidateUi.expandButton.imageResource =
            R.drawable.ic_baseline_expand_less_24
    }

    // should be used with setExpandButtonToAttach or setExpandButtonToDetach
    fun setExpandButtonEnabled(enabled: Boolean) {
        if (enabled)
            candidateUi.expandButton.visibility = View.VISIBLE
        else
            candidateUi.expandButton.visibility = View.INVISIBLE
    }

    fun updatePrivateModeIcon(editorInfo: EditorInfo?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            idleUi.privateMode(
                editorInfo?.imeOptions?.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) == true
            )
        }
    }

    private fun switchUi(ui: KawaiiBarUi, animation: Boolean = true) {
        if (currentUi == ui)
            return
        Timber.i("Switch bar to ${ui.javaClass.simpleName}")
        if (ui !is KawaiiBarUi.Title) {
            titleUi.setReturnButtonOnClickListener { }
            titleUi.setTitle("")
            titleUi.removeExtension()
        }
        if (animation) {
            prepareAnimation()
        }
        view.run {
            removeAllViews()
            add(ui.root, lParams(matchParent, matchParent))
        }
        currentUi = ui
    }

    override val view by lazy {
        context.frameLayout {
            currentUi = idleUi
            add(idleUi.root, lParams(matchParent, matchParent))
        }
    }


    override fun onScopeSetupFinished(scope: DynamicScope) {
        this.scope = scope
    }

    override fun onCandidateUpdates(data: Array<String>) {
        stateMachine.post(
            if (data.isEmpty())
                CandidatesUpdatedEmpty
            else
                CandidatesUpdatedNonEmpty
        )
    }

    override fun onWindowAttached(window: InputWindow) {
        when (window) {
            is InputWindow.ExtendedInputWindow<*> -> {
                titleUi.setTitle(window.title)
                window.barExtension?.let { titleUi.addExtension(it) }
                titleUi.setReturnButtonOnClickListener {
                    windowManager.switchToKeyboardWindow()
                }
                stateMachine.post(ExtendedWindowAttached)
            }
            is InputWindow.SimpleInputWindow<*> -> {
                stateMachine.post(SimpleWindowAttached)
            }
        }
    }

    override fun onWindowDetached(window: InputWindow) {
        stateMachine.post(WindowDetached)
    }

    private fun <T : Component> lazyComponent(block: () -> T) = lazy {
        block().also { scope += it }
    }
}