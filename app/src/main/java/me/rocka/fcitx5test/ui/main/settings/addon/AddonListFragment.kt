package me.rocka.fcitx5test.ui.main.settings.addon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.AddonInfo
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.ui.common.CheckBoxListUi
import me.rocka.fcitx5test.ui.common.OnItemChangedListener
import me.rocka.fcitx5test.ui.main.MainViewModel

class AddonListFragment : Fragment(), OnItemChangedListener<AddonInfo> {
    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    private val entries: List<AddonInfo>
        get() = ui.entries

    private val ui: CheckBoxListUi<AddonInfo> by lazy {
        CheckBoxListUi(
            ctx = requireContext(),
            initialEntries = listOf(),
            initCheckBox = {
                // our addon shouldn't be disabled
                isEnabled = entries[it].uniqueName != "androidfrontend"
                isChecked = entries[it].enabled
                setOnClickListener { _ ->
                    ui.updateItem(it, entries[it].copy(enabled = isChecked))
                }
            },
            initSettingsButton = { idx ->
                visibility =
                    if (entries[idx].isConfigurable && entries[idx].enabled) View.VISIBLE else View.INVISIBLE
                setOnClickListener {
                    it.findNavController().navigate(
                        R.id.action_addonListFragment_to_addonConfigFragment,
                        bundleOf(
                            AddonConfigFragment.ARG_UNIQUE_NAME to entries[idx].uniqueName,
                            AddonConfigFragment.ARG_NAME to entries[idx].displayName
                        )
                    )
                }
            },
            show = { it.displayName }
        )
    }

    private fun updateAddonState() {
        with(entries) {
            val ids = map { it.uniqueName }.toTypedArray()
            val state = map { it.enabled }.toBooleanArray()
            lifecycleScope.launch {
                fcitx.setAddonState(ids, state)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.disableToolbarSaveButton()
        lifecycleScope.launch {
            ui.entries = fcitx.addons().sortedBy { it.uniqueName }.toMutableList()
        }
        return ui.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.addons_conf))
    }

    override fun onItemUpdated(idx: Int, old: AddonInfo, new: AddonInfo) {
        updateAddonState()
    }

}