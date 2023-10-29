/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.addon

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.AddonInfo
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.CheckBoxListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment

class AddonListFragment : ProgressFragment(), OnItemChangedListener<AddonInfo> {

    private lateinit var ui: BaseDynamicListUi<AddonInfo>

    private val addonDisplayNames = mutableMapOf<String, String>()

    private fun updateAddonState() {
        if (!isInitialized) return
        val ids = ui.entries.map { it.uniqueName }.toTypedArray()
        val state = ui.entries.map { it.enabled }.toBooleanArray()
        fcitx.launchOnReady {
            it.setAddonState(ids, state)
        }
    }

    private fun disableAddon(entry: AddonInfo, reset: () -> Unit) {
        val dependents = fcitx.runImmediately { getAddonReverseDependencies(entry.uniqueName) }
        if (dependents.isNotEmpty()) {
            fun f(depTy: FcitxAPI.AddonDep) =
                dependents.mapNotNull {
                    it.takeIf { x -> x.second == depTy }
                        ?.first
                        ?.let { u -> it.first to (addonDisplayNames[u] ?: u) }
                }

            fun mkStr(list: List<Pair<String, String>>, @StringRes template: Int) =
                list.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { it.second }
                    ?.let { getString(template, it) }

            val dep = f(FcitxAPI.AddonDep.Required)
            val depU = dep.map { it.first }.toSet()
            val depStr = mkStr(dep, R.string.disable_addon_warn_dep)
            val optDepStr = mkStr(f(FcitxAPI.AddonDep.Optional), R.string.disable_addon_warn_optdep)

            if (depStr != null || optDepStr != null) {
                val msg = buildString {
                    appendLine(getString(R.string.disable_addon_warn_name, entry.displayName))
                    depStr?.let {
                        append("- ")
                        appendLine(it)
                    }
                    optDepStr?.let {
                        append("- ")
                        appendLine(it)
                    }
                    appendLine(getString(R.string.disable_addon_warn_confirm))
                }
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.disable_addon_warn_title))
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(msg)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        reset()
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        ui.entries.forEachIndexed { idx, addonInfo ->
                            // TODO: combine update addon states
                            if (addonInfo.uniqueName in depU) {
                                ui.updateItem(idx, addonInfo.copy(enabled = false))
                            }
                        }
                        ui.updateItem(ui.indexItem(entry), entry.copy(enabled = false))
                    }
                    .show()
            } else {
                ui.updateItem(ui.indexItem(entry), entry.copy(enabled = false))
            }
        } else {
            ui.updateItem(ui.indexItem(entry), entry.copy(enabled = false))
        }
    }

    override suspend fun initialize(): View {
        ui = requireContext().CheckBoxListUi(
            initialEntries = fcitx.runOnReady {
                addons()
                    .sortedBy { it.uniqueName }
                    .onEach { addonDisplayNames[it.uniqueName] = it.displayName }
            },
            initCheckBox = { entry ->
                // remove old listener before change checked state
                setOnCheckedChangeListener(null)
                // our addon shouldn't be disabled
                isEnabled = entry.uniqueName != "androidfrontend"
                isChecked = entry.enabled
                setOnCheckedChangeListener { _, isChecked ->
                    if (!isChecked)
                        disableAddon(entry) { this.isChecked = true }
                    else
                        ui.updateItem(ui.indexItem(entry), entry.copy(enabled = true))
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

    override fun onItemUpdated(idx: Int, old: AddonInfo, new: AddonInfo) {
        updateAddonState()
    }

    override fun onDestroy() {
        if (isInitialized) {
            ui.removeItemChangedListener()
        }
        super.onDestroy()
    }

}