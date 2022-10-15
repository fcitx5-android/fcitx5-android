package org.fcitx.fcitx5.android.ui.main.settings.addon

import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.AddonInfo
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.CheckBoxListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment

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
                fcitx.runOnReady {
                    setAddonState(ids, state)
                }
            }
        }
    }

    override suspend fun initialize(): View {
        ui = requireContext().CheckBoxListUi(
            initialEntries = fcitx.runOnReady { addons().sortedBy { it.uniqueName } },
            initCheckBox = { entry ->
                // remove old listener before change checked state
                setOnCheckedChangeListener(null)
                // our addon shouldn't be disabled
                isEnabled = entry.uniqueName != "androidfrontend"
                isChecked = entry.enabled
                setOnCheckedChangeListener { _, isChecked ->
                    ui.updateItem(ui.indexItem(entry), entry.copy(enabled = isChecked))
                }
            },
            initSettingsButton = { entry ->
                visibility =
                    if (entry.isConfigurable &&
                        entry.enabled &&
                        // we disable clipboard addon config since we take over the control
                        entry.uniqueName != "clipboard"
                    ) View.VISIBLE else View.INVISIBLE
                setOnClickListener {
                    it.findNavController().navigate(
                        R.id.action_addonListFragment_to_addonConfigFragment,
                        bundleOf(
                            AddonConfigFragment.ARG_UNIQUE_NAME to entry.uniqueName,
                            AddonConfigFragment.ARG_NAME to entry.displayName
                        )
                    )
                }
            },
            show = { it.displayName }
        )
        ui.addOnItemChangedListener(this)
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