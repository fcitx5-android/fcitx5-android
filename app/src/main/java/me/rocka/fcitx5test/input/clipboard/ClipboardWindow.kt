package me.rocka.fcitx5test.input.clipboard

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.clipboard.ClipboardManager
import me.rocka.fcitx5test.input.FcitxInputMethodService
import me.rocka.fcitx5test.input.dependency.inputMethodService
import me.rocka.fcitx5test.input.wm.InputWindow
import me.rocka.fcitx5test.utils.inputConnection
import me.rocka.fcitx5test.utils.onDataChanged
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.lParams
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageResource

class ClipboardWindow : InputWindow.ExtendedInputWindow<ClipboardWindow>() {

    private val service: FcitxInputMethodService by manager.inputMethodService()

    val layoutManager by lazy {
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }
    private val onClipboardUpdateListener by lazy {
        ClipboardManager.OnClipboardUpdateListener {
            service.lifecycleScope.launch {
                adapter.updateEntries(ClipboardManager.getAll())
            }
        }
    }

    val adapter: ClipboardAdapter by lazy {
        object : ClipboardAdapter() {
            init {
                onDataChanged {
                    updateDeleteButton(adapter.itemCount)
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
    }

    val deleteAllButton by lazy {
        context.imageButton {
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            imageResource = R.drawable.ic_baseline_delete_sweep_24
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            visibility = View.INVISIBLE
            setOnClickListener {
                service.lifecycleScope.launch {
                    ClipboardManager.deleteAll()
                    adapter.updateEntries(emptyList())
                }
            }
        }
    }

    private fun updateDeleteButton(itemCount: Int) {
        deleteAllButton.visibility = if (itemCount > 0) View.VISIBLE else View.INVISIBLE
    }

    override val view by lazy {
        context.recyclerView {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            layoutManager = this@ClipboardWindow.layoutManager
            adapter = this@ClipboardWindow.adapter
            addItemDecoration(SpacesItemDecoration(dp(4)))
        }
    }

    override fun onAttached() {
        service.lifecycleScope.launch {
            adapter.updateEntries(ClipboardManager.getAll())
        }
        ClipboardManager.addOnUpdateListener(onClipboardUpdateListener)
    }

    override fun onDetached() {
        ClipboardManager.removeOnUpdateListener(onClipboardUpdateListener)
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override val barExtension: View by lazy {
        context.horizontalLayout {
            add(deleteAllButton, lParams(dp(40), dp(40)))
        }
    }
}