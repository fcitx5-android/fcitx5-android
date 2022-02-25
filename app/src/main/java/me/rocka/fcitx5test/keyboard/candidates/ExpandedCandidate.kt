package me.rocka.fcitx5test.keyboard.candidates

import android.content.Context
import me.rocka.fcitx5test.utils.dependency.context
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class ExpandedCandidate : UniqueComponent<ExpandedCandidate>(), Dependent,
    ManagedHandler by managedHandler() {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()

    val adapter by lazy { builder.newCandidateViewAdapter() }
    val ui by lazy {
        ExpandedCandidateUi(context) {
            with(builder) {
                autoSpanCount()
                setupGridLayoutManager(this@ExpandedCandidate.adapter, true)
                addGridDecoration()
            }
        }
    }
}