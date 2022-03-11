package me.rocka.fcitx5test.input.candidates

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.input.bar.KawaiiBarComponent
import me.rocka.fcitx5test.input.broadcast.InputBroadcastReceiver
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.wm.InputWindowManager
import me.rocka.fcitx5test.utils.globalLayoutListener
import me.rocka.fcitx5test.utils.onDataChanged
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.recyclerview.recyclerView
import java.util.concurrent.atomic.AtomicBoolean

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>(), InputBroadcastReceiver {

    private val builder: CandidateViewBuilder by manager.must()
    private val context: Context by manager.context()
    private val bar: KawaiiBarComponent by manager.must()
    private val windowManager: InputWindowManager by manager.must()

    private var needsRefreshExpanded = AtomicBoolean(false)

    val adapter by lazy {
        builder.simpleAdapter().apply {
            onDataChanged {
                needsRefreshExpanded.set(true)
            }
        }
    }

    private val _expandedCandidateOffset = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val expandedCandidateOffset = _expandedCandidateOffset.asSharedFlow()

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
                    // decide do we need to show expand candidate button or close expanded candidate
                    if (candidates.size - childCount > 0) {
                        // enable the button
                        // expand candidate will be created when the button is clicked
                        bar.setExpandButtonEnabled(true)
                    } else {
                        // close expand candidate and disable the button
                        windowManager.switchToKeyboardWindow()
                        bar.setExpandButtonEnabled(false)
                    }
                    runBlocking {
                        _expandedCandidateOffset.emit(childCount)
                    }
                }
            }
        }
    }

    override fun onCandidateUpdates(data: Array<String>) {
        adapter.updateCandidates(data)
    }
}