package me.rocka.fcitx5test.ui.main.settings.im

import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.core.InputMethodEntry
import me.rocka.fcitx5test.ui.common.BaseDynamicListUi
import me.rocka.fcitx5test.ui.common.DynamicListTouchCallback
import me.rocka.fcitx5test.ui.common.DynamicListUi
import me.rocka.fcitx5test.ui.common.OnItemChangedListener
import me.rocka.fcitx5test.ui.main.settings.ProgressFragment

class InputMethodListFragment : ProgressFragment(), OnItemChangedListener<InputMethodEntry> {

    val entries: List<InputMethodEntry>
        get() = ui.entries

    private fun updateIMState() {
        if (isInitialized)
            lifecycleScope.launch {
                fcitx.setEnabledIme(entries.map { it.uniqueName }.toTypedArray())
            }
    }

    private lateinit var ui: BaseDynamicListUi<InputMethodEntry>

    override suspend fun initialize(): View {
        val available = fcitx.availableIme().toSet()
        val enabled = fcitx.enabledIme().toList()
        ui = requireContext().DynamicListUi(
            mode = BaseDynamicListUi.Mode.ChooseOne {
                val unEnabled = available - entries.toSet()
                unEnabled.toTypedArray()
            },
            initialEntries = enabled,
            enableOrder = true,
            initSettingsButton = { idx ->
                val entry = entries[idx]
                setOnClickListener {
                    it.findNavController().navigate(
                        R.id.action_imListFragment_to_imConfigFragment,
                        bundleOf(
                            InputMethodConfigFragment.ARG_UNIQUE_NAME to entry.uniqueName,
                            InputMethodConfigFragment.ARG_NAME to entry.displayName
                        )
                    )
                }
            },
            useCustomTouchCallback = true,
            show = { it.displayName }
        )
        ui.addOnItemChangedListener(this@InputMethodListFragment)
        ui.addTouchCallback(object : DynamicListTouchCallback<InputMethodEntry>(ui) {

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // English keyboard shouldn't be removed
                if (ui.entries[viewHolder.bindingAdapterPosition].uniqueName == "keyboard-us")
                    return if (ui.enableOrder) ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    else ItemTouchHelper.ACTION_STATE_IDLE
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
        })
        return ui.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(requireContext().getString(R.string.input_methods_conf))
    }

    override fun onItemSwapped(fromIdx: Int, toIdx: Int, item: InputMethodEntry) {
        updateIMState()
    }

    override fun onItemAdded(idx: Int, item: InputMethodEntry) {
        updateIMState()
    }

    override fun onItemRemoved(idx: Int, item: InputMethodEntry) {
        updateIMState()
    }

    override fun onItemUpdated(idx: Int, old: InputMethodEntry, new: InputMethodEntry) {
        updateIMState()
    }
}