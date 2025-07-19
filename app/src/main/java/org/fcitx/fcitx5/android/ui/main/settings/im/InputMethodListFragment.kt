/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.im

import android.os.Build
import android.view.View
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.SubtypeManager
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.DynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.navigateWithAnim

class InputMethodListFragment : ProgressFragment(), OnItemChangedListener<InputMethodEntry> {

    private fun updateIMState() {
        if (isInitialized) {
            fcitx.launchOnReady { f ->
                f.setEnabledIme(ui.entries.map { it.uniqueName }.toTypedArray())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SubtypeManager.syncWith(f.enabledIme())
                }
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
                    navigateWithAnim(
                        SettingsRoute.InputMethodConfig(entry.displayName, entry.uniqueName),
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