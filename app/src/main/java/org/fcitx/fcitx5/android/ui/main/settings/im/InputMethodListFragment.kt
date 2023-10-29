/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.im

import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.DynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment

class InputMethodListFragment : ProgressFragment(), OnItemChangedListener<InputMethodEntry> {

    private fun updateIMState() {
        if (isInitialized) {
            fcitx.launchOnReady { f ->
                f.setEnabledIme(ui.entries.map { it.uniqueName }.toTypedArray())
            }
        }
    }

    private lateinit var ui: BaseDynamicListUi<InputMethodEntry>

    override suspend fun initialize(): View {
        val available = fcitx.runOnReady { availableIme().toSet() }
        val initialEnabled = fcitx.runOnReady { enabledIme().toList() }
        ui = requireContext().DynamicListUi(
            mode = BaseDynamicListUi.Mode.ChooseOne {
                (available - entries.toSet()).toTypedArray()
            },
            initialEntries = initialEnabled,
            enableOrder = true,
            initSettingsButton = { entry ->
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
            show = { it.displayName }
        )
        ui.addOnItemChangedListener(this@InputMethodListFragment)
        ui.setViewModel(viewModel)
        viewModel.enableToolbarEditButton(initialEnabled.isNotEmpty()) {
            ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
        }
        return ui.root
    }

    override fun onStart() {
        super.onStart()
        if (::ui.isInitialized) {
            viewModel.enableToolbarEditButton(ui.entries.isNotEmpty()) {
                ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
            }
        }
    }

    override fun onStop() {
        if (::ui.isInitialized) {
            ui.exitMultiSelect()
        }
        viewModel.disableToolbarEditButton()
        super.onStop()
    }

    override fun onDestroy() {
        if (::ui.isInitialized) {
            ui.removeItemChangedListener()
        }
        super.onDestroy()
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

    override fun onItemRemovedBatch(indexed: List<Pair<Int, InputMethodEntry>>) {
        batchRemove(indexed)
    }

    override fun onItemUpdated(idx: Int, old: InputMethodEntry, new: InputMethodEntry) {
        updateIMState()
    }
}