package me.rocka.fcitx5test.settings.im

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import me.rocka.fcitx5test.MainViewModel
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.native.InputMethodEntry
import me.rocka.fcitx5test.ui.olist.BaseOrderedListUi
import me.rocka.fcitx5test.ui.olist.OnItemChangedListener

class InputMethodListFragment : Fragment(), OnItemChangedListener<InputMethodEntry> {

    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    val entries: List<InputMethodEntry>
        get() = ui.entries


    private fun updateIMState() {
        Log.d(javaClass.name, "IM state updated: ${entries.joinToString { it.displayName }}")
        fcitx.setEnabledIme(entries.map { it.uniqueName }.toTypedArray())
    }

    private val ui: BaseOrderedListUi<InputMethodEntry> by lazy {
        object : BaseOrderedListUi<InputMethodEntry>(
            requireContext(),
            Mode.ChooseOne {
                val unEnabled = fcitx.availableIme().toSet() - entries.toSet()
                unEnabled.toTypedArray()
            }, fcitx.enabledIme().toList(), true, { idx ->
                setOnClickListener {
                    it.findNavController().navigate(
                        R.id.action_imListFragment_to_imConfigFragment,
                        bundleOf(
                            InputMethodConfigFragment.ARG_UNIQUE_NAME to entries[idx].uniqueName,
                            InputMethodConfigFragment.ARG_NAME to entries[idx].displayName
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
        viewModel.setToolbarTitle(requireContext().getString(R.string.input_methods_conf))
        viewModel.disableToolbarSaveButton()
        ui.addOnItemChangedListener(this)
        return ui.root
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