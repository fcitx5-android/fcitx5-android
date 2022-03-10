package me.rocka.fcitx5test.input.bar

import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.candidates.ExpandableCandidateComponent
import me.rocka.fcitx5test.input.candidates.HorizontalCandidateComponent
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.wm.InputWindow
import me.rocka.fcitx5test.input.wm.InputWindowManager
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import org.mechdancer.dependency.plusAssign
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.imageResource

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, FrameLayout>(),
    InputBroadcastReceiver {

    private val context by manager.context()

    // TODO: use windowManager here to switch input windows
    private val windowManager: InputWindowManager by manager.must()

    private lateinit var horizontalCandidate: HorizontalCandidateComponent

    private lateinit var expandableCandidate: ExpandableCandidateComponent

    private val idleBarUi by lazy { IdleBarUi(context) }

    private lateinit var candidateBarUi: CandidateBarUi

    private val titleBarUi by lazy { TitleBarUi(context) }

    override fun onScopeSetupFinished(scope: DynamicScope) {
        horizontalCandidate = HorizontalCandidateComponent()
        scope += horizontalCandidate
        candidateBarUi = CandidateBarUi(context, horizontalCandidate.view)
        expandableCandidate = ExpandableCandidateComponent {
            if (adapter.itemCount == 0) {
                shrink()
                candidateBarUi.expandButton.visibility = ConstraintLayout.INVISIBLE
            } else {
                candidateBarUi.expandButton.visibility = ConstraintLayout.VISIBLE
            }
        }
        scope += expandableCandidate
        expandableCandidate.init()
        expandableCandidate.onStateUpdate = {
            when (it) {
                ExpandableCandidateComponent.State.Expanded -> {
                    candidateBarUi.expandButton.setOnClickListener { expandableCandidate.shrink() }
                    candidateBarUi.expandButton.imageResource = R.drawable.ic_baseline_expand_less_24
                }
                ExpandableCandidateComponent.State.Shrunk -> {
                    candidateBarUi.expandButton.setOnClickListener { expandableCandidate.expand() }
                    candidateBarUi.expandButton.imageResource = R.drawable.ic_baseline_expand_more_24
                }
            }
        }
    }

    override val view by lazy {
        context.frameLayout {
            add(idleBarUi.root, lParams(matchParent, matchParent))
        }
    }

    override fun onWindowAttached(window: InputWindow<*>) {
        // TODO: display window.title and window.barExtension
    }

    override fun onWindowDetached(window: InputWindow<*>) {
        // TODO: cleanup
    }
}