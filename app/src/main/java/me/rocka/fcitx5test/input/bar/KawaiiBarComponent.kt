package me.rocka.fcitx5test.input.bar

import android.view.View
import android.widget.FrameLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.candidates.ExpandedCandidateWindow
import me.rocka.fcitx5test.input.candidates.HorizontalCandidateComponent
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.dependency.inputMethodService
import me.rocka.fcitx5test.input.wm.InputWindow
import me.rocka.fcitx5test.input.wm.InputWindowManager
import org.mechdancer.dependency.Component
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import org.mechdancer.dependency.plusAssign
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource
import timber.log.Timber
import kotlin.reflect.KClass

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

    private fun prepareAnimation() {
        val transition = AutoTransition()
        transition.duration = 50
        TransitionManager.beginDelayedTransition(view, transition)
    }

    fun switchUi(uiClass: KClass<out KawaiiBarUi>, animation: Boolean = true) {
        when (uiClass.java) {
            idleUi.javaClass -> switchUi(idleUi, animation)
            candidateUi.javaClass -> switchUi(candidateUi, animation)
            titleUi.javaClass -> switchUi(titleUi, animation)
            else -> throw IllegalArgumentException("Can not switch to $uiClass")
        }
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
        // go back to idle ui if there's no candidate
        // this is the only way candidate bar can be replaced
        if (data.isEmpty() && currentUi is KawaiiBarUi.Candidate)
            switchUi(idleUi)
        if (data.isNotEmpty())
            switchUi(candidateUi)
    }

    override fun onWindowAttached(window: InputWindow) {
        when (window) {
            is InputWindow.ExtendedInputWindow<*> -> {
                titleUi.setTitle(window.title)
                window.barExtension?.let { titleUi.addExtension(it) }
                titleUi.setReturnButtonOnClickListener {
                    windowManager.switchToKeyboardWindow()
                }
                // no window can replace the candidate bar
                if (currentUi is KawaiiBarUi.Candidate)
                    throw IllegalStateException("The title on extended $window is conflict with candidate bar")
                else
                    switchUi(titleUi)
            }
            is InputWindow.SimpleInputWindow<*> -> {
                // no window can replace the candidate bar
                if (currentUi !is KawaiiBarUi.Candidate)
                    switchUi(idleUi)
            }
        }
    }

    override fun onWindowDetached(window: InputWindow) {
        // no window can replace the candidate bar
        if (currentUi !is KawaiiBarUi.Candidate) {
            switchUi(idleUi)
        }
    }

    private fun <T : Component> lazyComponent(block: () -> T) = lazy {
        block().also { scope += it }
    }
}