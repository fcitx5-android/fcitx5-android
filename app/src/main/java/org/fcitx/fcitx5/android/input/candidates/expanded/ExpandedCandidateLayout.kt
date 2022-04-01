package org.fcitx.fcitx5.android.input.candidates.expanded

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.keyboard.*
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.recyclerview.recyclerView

@SuppressLint("ViewConstructor")
class ExpandedCandidateLayout(
    context: Context,
    initRecyclerView: RecyclerView.() -> Unit = {}
) : LinearLayout(context) {

    class Keyboard(context: Context) : BaseKeyboard(context, Layout) {
        companion object {
            const val UpBtnLabel = "U"
            const val DownBtnLabel = "D"

            const val UpBtnId = 0xff55
            const val DownBtnId = 0xff56

            val Layout: List<List<KeyDef>> = listOf(
                listOf(
                    ImageLayoutSwitchKey(
                        R.drawable.ic_baseline_arrow_upward_24,
                        to = UpBtnLabel,
                        percentWidth = 1f,
                        viewId = UpBtnId
                    )
                ),
                listOf(
                    ImageLayoutSwitchKey(
                        R.drawable.ic_baseline_arrow_downward_24,
                        to = DownBtnLabel,
                        percentWidth = 1f,
                        viewId = DownBtnId
                    )
                ),
                listOf(BackspaceKey(percentWidth = 1f)),
                listOf(ReturnKey(percentWidth = 1f))
            )
        }

        val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
        val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }
    }

    val recyclerView = recyclerView {
        isVerticalScrollBarEnabled = false
    }

    var pageUpBtn: ImageKeyView

    var pageDnBtn: ImageKeyView

    val embeddedKeyboard = Keyboard(context).apply {
        pageUpBtn = findViewById(Keyboard.UpBtnId)
        pageDnBtn = findViewById(Keyboard.DownBtnId)
    }

    init {
        id = R.id.expanded_candidate_view
        weightSum = 1f
        orientation = HORIZONTAL
        backgroundColor = styledColor(android.R.attr.colorBackground)

        add(recyclerView, lParams(0, matchParent) { weight = 0.85f })
        add(embeddedKeyboard, lParams(0, matchParent) { weight = 0.15f })

        initRecyclerView(recyclerView)
    }

    fun resetPosition() {
        recyclerView.scrollToPosition(0)
    }
}