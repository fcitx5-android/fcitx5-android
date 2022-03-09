package me.rocka.fcitx5test.input.clipboard

import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.clipboard.ClipboardManager
import me.rocka.fcitx5test.input.FcitxInputMethodService
import me.rocka.fcitx5test.input.dependency.UniqueViewComponent
import me.rocka.fcitx5test.input.dependency.context
import me.rocka.fcitx5test.input.dependency.inputMethodService
import me.rocka.fcitx5test.utils.inputConnection
import me.rocka.fcitx5test.utils.onDataChanged
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource

class ClipboardComponent : UniqueViewComponent<ClipboardComponent, LinearLayout>() {

    private val context by manager.context()
    private val service: FcitxInputMethodService by manager.inputMethodService()

    val layoutManager by lazy {
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }
    val adapter =
        object : ClipboardAdapter(runBlocking { ClipboardManager.getAll() }) {
            init {
                ClipboardManager.addOnUpdateListener {
                    service.lifecycleScope.launch {
                        updateEntries(ClipboardManager.getAll())
                    }
                }
                onDataChanged {
                    layoutManager.invalidateSpanAssignments()
                }
            }

            override fun onPaste(id: Int) {
                service.inputConnection?.commitText(getEntryById(id).text, 1)
            }

            override suspend fun onPin(id: Int) {
                ClipboardManager.pin(id)
            }

            override suspend fun onUnpin(id: Int) {
                ClipboardManager.unpin(id)
            }

            override suspend fun onDelete(id: Int) {
                ClipboardManager.delete(id)
            }

        }

    val deleteAllButton by lazy {
        context.imageButton {
            background = styledDrawable(android.R.attr.selectableItemBackground)
            imageResource = R.drawable.ic_baseline_delete_sweep_24
            setOnClickListener {
                service.lifecycleScope.launch {
                    ClipboardManager.deleteAll()
                    adapter.updateEntries(emptyList())
                }
            }
        }
    }

    val recyclerView: RecyclerView by lazy {
        context.recyclerView {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            layoutManager = this@ClipboardComponent.layoutManager
            adapter = this@ClipboardComponent.adapter
            addItemDecoration(SpacesItemDecoration(dp(2)))
        }
    }

    override val view by lazy {
        // TODO: Fix layout
        context.verticalLayout {
            add(horizontalLayout {
                add(deleteAllButton, lParams(wrapContent, wrapContent))
            }, lParams(matchParent, wrapContent))
            add(recyclerView, lParams(matchParent, matchParent))
        }
    }
}