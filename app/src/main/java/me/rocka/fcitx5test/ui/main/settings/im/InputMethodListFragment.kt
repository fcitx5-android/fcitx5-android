package me.rocka.fcitx5test.ui.main.settings.im

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.InputMethodEntry
import me.rocka.fcitx5test.ui.common.BaseDynamicListUi
import me.rocka.fcitx5test.ui.common.OnItemChangedListener
import me.rocka.fcitx5test.ui.main.MainViewModel

class InputMethodListFragment : Fragment(), OnItemChangedListener<InputMethodEntry> {

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    val entries: List<InputMethodEntry>
        get() = ui.entries


    private fun updateIMState() {
        fcitx.setEnabledIme(entries.map { it.uniqueName }.toTypedArray())
    }

    private val ui: BaseDynamicListUi<InputMethodEntry> by lazy {
        object : BaseDynamicListUi<InputMethodEntry>(
            ctx = requireContext(),
            mode = Mode.ChooseOne {
                val unEnabled = fcitx.availableIme().toSet() - entries.toSet()
                unEnabled.toTypedArray()
            },
            initialEntries = fcitx.enabledIme().toList(),
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
            }
        ) {
            override fun showEntry(x: InputMethodEntry): String = x.displayName
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.disableToolbarSaveButton()
        ui.addOnItemChangedListener(this)
        return ui.root
    }

    override fun onResume() {
        super.onResume()
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