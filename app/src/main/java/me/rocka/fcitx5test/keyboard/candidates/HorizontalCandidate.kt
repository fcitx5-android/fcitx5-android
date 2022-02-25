package me.rocka.fcitx5test.keyboard.candidates

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.utils.dependency.context
import me.rocka.fcitx5test.utils.globalLayoutListener
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.recyclerview.recyclerView
import java.util.concurrent.atomic.AtomicBoolean

class HorizontalCandidate : UniqueComponent<HorizontalCandidate>(), Dependent,
    ManagedHandler by managedHandler() {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()
    private val expandedCandidate: ExpandedCandidate by manager.must()

    private var needsRefreshExpanded = AtomicBoolean(false)
    val adapter by lazy {
        builder.newCandidateViewAdapter().also {
            it.registerAdapterDataObserver(object :
                RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    needsRefreshExpanded.set(true)
                }
            })
        }
    }
    val recyclerView by lazy {
        context.recyclerView(R.id.candidate_view) {
            isVerticalScrollBarEnabled = false
            backgroundColor = styledColor(android.R.attr.colorBackground)
            with(builder) {
                autoSpanCount()
                setupGridLayoutManager(this@HorizontalCandidate.adapter, false)
                addGridDecoration()
            }
            globalLayoutListener {
                if (needsRefreshExpanded.compareAndSet(true, false)) {
                    val candidates = this@HorizontalCandidate.adapter.candidates
                    expandedCandidate.adapter.updateCandidates(
                        candidates.sliceArray(childCount until candidates.size)
                    )
                }
            }
        }
    }
}