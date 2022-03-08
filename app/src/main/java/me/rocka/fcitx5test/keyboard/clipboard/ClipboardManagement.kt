package me.rocka.fcitx5test.keyboard.clipboard

import android.graphics.Rect
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.rocka.fcitx5test.data.clipboard.ClipboardManager
import me.rocka.fcitx5test.keyboard.FcitxInputMethodService
import me.rocka.fcitx5test.utils.dependency.context
import me.rocka.fcitx5test.utils.dependency.service
import me.rocka.fcitx5test.utils.inputConnection
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.recyclerview.recyclerView

class ClipboardManagement : UniqueComponent<ClipboardManagement>(), Dependent,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val service: FcitxInputMethodService by manager.service()

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

    val layout: RecyclerView by lazy {
        context.recyclerView {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            layoutManager = this@ClipboardManagement.layoutManager
            adapter = this@ClipboardManagement.adapter
            addItemDecoration(SpacesItemDecoration(dp(2)))
        }
    }

    class SpacesItemDecoration(val space: Int) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) = outRect.run {
            top = space
            bottom = space
            left = space
            right = space
        }
    }

}