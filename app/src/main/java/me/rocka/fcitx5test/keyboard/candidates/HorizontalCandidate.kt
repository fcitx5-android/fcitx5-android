package me.rocka.fcitx5test.keyboard.candidates

import android.content.Context
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.utils.dependency.context
import me.rocka.fcitx5test.utils.globalLayoutListener
import me.rocka.fcitx5test.utils.onDataChanged
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.recyclerview.recyclerView
import java.util.concurrent.atomic.AtomicBoolean

class HorizontalCandidate : UniqueComponent<HorizontalCandidate>(), Dependent,
    ManagedHandler by managedHandler() {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()
    private val expandableCandidate: ExpandableCandidate by manager.must()

    private var needsRefreshExpanded = AtomicBoolean(false)

    val adapter by lazy {
        builder.simpleAdapter().apply {
            onDataChanged {
                needsRefreshExpanded.set(true)
            }
        }
    }

    val recyclerView by lazy {
        context.recyclerView(R.id.candidate_view) {
            isVerticalScrollBarEnabled = false
            with(builder) {
                setupFlexboxLayoutManager(this@HorizontalCandidate.adapter, false)
                addVerticalDecoration()
            }
            globalLayoutListener {
                if (needsRefreshExpanded.compareAndSet(true, false)) {
                    val candidates = this@HorizontalCandidate.adapter.candidates
                    expandableCandidate.adapter.updateCandidatesWithOffset(candidates, childCount)
                }
            }
        }
    }
}