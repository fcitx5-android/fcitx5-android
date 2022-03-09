package me.rocka.fcitx5test.input.candidates

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.utils.globalLayoutListener
import me.rocka.fcitx5test.utils.onDataChanged
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.recyclerview.recyclerView
import java.util.concurrent.atomic.AtomicBoolean

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>(), InputBroadcastReceiver {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()
    private val expandableCandidate: ExpandableCandidateComponent by manager.must()

    private var needsRefreshExpanded = AtomicBoolean(false)

    val adapter by lazy {
        builder.simpleAdapter().apply {
            onDataChanged {
                needsRefreshExpanded.set(true)
            }
        }
    }

    override val view by lazy {
        context.recyclerView(R.id.candidate_view) {
            isVerticalScrollBarEnabled = false
            with(builder) {
                setupFlexboxLayoutManager(this@HorizontalCandidateComponent.adapter, false)
                addFlexboxVerticalDecoration()
            }
            globalLayoutListener {
                if (needsRefreshExpanded.compareAndSet(true, false)) {
                    val candidates = this@HorizontalCandidateComponent.adapter.candidates
                    expandableCandidate.adapter.updateCandidatesWithOffset(candidates, childCount)
                }
            }
        }
    }

    override fun onCandidateUpdates(data: Array<String>) {
        adapter.updateCandidates(data)
    }
}