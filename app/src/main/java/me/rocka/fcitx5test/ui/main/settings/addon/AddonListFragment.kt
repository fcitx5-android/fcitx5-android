package me.rocka.fcitx5test.ui.main.settings.addon

import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.native.AddonInfo
import me.rocka.fcitx5test.ui.common.BaseDynamicListUi
import me.rocka.fcitx5test.ui.common.CheckBoxListUi
import me.rocka.fcitx5test.ui.common.OnItemChangedListener
import me.rocka.fcitx5test.ui.main.settings.ProgressFragment

class AddonListFragment : ProgressFragment(), OnItemChangedListener<AddonInfo> {

    private val entries: List<AddonInfo>
        get() = ui.entries

    private lateinit var ui: BaseDynamicListUi<AddonInfo>

    private fun updateAddonState() {
        if (!isInitialized)
            return
        with(entries) {
            val ids = map { it.uniqueName }.toTypedArray()
            val state = map { it.enabled }.toBooleanArray()
            lifecycleScope.launch {
                fcitx.setAddonState(ids, state)
            }
        }
    }


    override suspend fun initialize(): View {
        ui = requireContext().CheckBoxListUi(
            initialEntries = fcitx.addons().sortedBy { it.uniqueName },
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
        return ui.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.addons_conf))
        viewModel.disableToolbarSaveButton()
    }

    override fun onItemUpdated(idx: Int, old: AddonInfo, new: AddonInfo) {
        updateAddonState()
    }

}