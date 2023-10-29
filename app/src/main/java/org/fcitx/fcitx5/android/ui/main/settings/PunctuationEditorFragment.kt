/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.core.getPunctuationConfig
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager
import org.fcitx.fcitx5.android.data.punctuation.PunctuationMapEntry
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.materialTextInput
import org.fcitx.fcitx5.android.utils.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.setPaddingDp

class PunctuationEditorFragment : ProgressFragment(), OnItemChangedListener<PunctuationMapEntry> {

    private lateinit var lang: String
    private lateinit var keyDesc: String
    private lateinit var mappingDesc: String
    private lateinit var altMappingDesc: String

    private val dustman = NaiveDustman<PunctuationMapEntry>()

    private fun findDesc(raw: RawConfig) {
        // parse config desc to get description text of the options
        raw["desc"][PunctuationManager.MAP_ENTRY_CONFIG].subItems!!.forEach {
            val desc = it["Description"].value
            when (it.name) {
                PunctuationManager.KEY -> keyDesc = desc
                PunctuationManager.MAPPING -> mappingDesc = desc
                PunctuationManager.ALT_MAPPING -> altMappingDesc = desc
            }
        }
    }

    private fun saveConfig() {
        if (!dustman.dirty) return
        resetDustman()
        fcitx.launchOnReady {
            PunctuationManager.save(it, lang, ui.entries)
        }
    }

    private lateinit var ui: BaseDynamicListUi<PunctuationMapEntry>

    private fun resetDustman() {
        dustman.reset(ui.entries.associateBy { it.key })
    }

    override suspend fun initialize(): View {
        lang = requireArguments().getString(LANG, DEFAULT_LANG)
        val raw = fcitx.runOnReady { getPunctuationConfig(lang) }
        findDesc(raw)
        val initialEntries = PunctuationManager.parseRawConfig(raw)
        ui = object : BaseDynamicListUi<PunctuationMapEntry>(
            requireContext(),
            Mode.FreeAdd(hint = "", converter = { PunctuationMapEntry(it, "", "") }),
            initialEntries,
            enableOrder = true
        ) {
            init {
                addTouchCallback()
                addOnItemChangedListener(this@PunctuationEditorFragment)
                setViewModel(viewModel)
            }

            override fun showEntry(x: PunctuationMapEntry) = x.run {
                "$key\u2003→\u2003$mapping $altMapping"
            }

            override fun showEditDialog(
                title: String,
                entry: PunctuationMapEntry?,
                block: (PunctuationMapEntry) -> Unit
            ) {
                val (keyLayout, keyField) = materialTextInput {
                    hint = keyDesc
                }
                val (mappingLayout, mappingField) = materialTextInput {
                    hint = mappingDesc
                }
                val (altMappingLayout, altMappingField) = materialTextInput {
                    hint = altMappingDesc
                }
                entry?.apply {
                    keyField.setText(key)
                    mappingField.setText(mapping)
                    altMappingField.setText(altMapping)
                }
                val layout = verticalLayout {
                    setPaddingDp(20, 10, 20, 0)
                    add(keyLayout, lParams(matchParent))
                    add(mappingLayout, lParams(matchParent))
                    add(altMappingLayout, lParams(matchParent))
                }
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        block(
                            PunctuationMapEntry(keyField.str, mappingField.str, altMappingField.str)
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        resetDustman()
        viewModel.enableToolbarEditButton(initialEntries.isNotEmpty()) {
            ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
        }
        return ui.root
    }

    override fun onItemAdded(idx: Int, item: PunctuationMapEntry) {
        dustman.addOrUpdate(item.key, item)
    }

    override fun onItemRemoved(idx: Int, item: PunctuationMapEntry) {
        dustman.remove(item.key)
    }

    override fun onItemRemovedBatch(indexed: List<Pair<Int, PunctuationMapEntry>>) {
        batchRemove(indexed)
    }

    override fun onItemUpdated(idx: Int, old: PunctuationMapEntry, new: PunctuationMapEntry) {
        dustman.addOrUpdate(new.key, new)
    }

    override fun onItemSwapped(fromIdx: Int, toIdx: Int, item: PunctuationMapEntry) {
        dustman.forceDirty()
    }

    override fun onStart() {
        super.onStart()
        viewModel.setToolbarTitle(requireArguments().getString(TITLE)!!)
        if (isInitialized) {
            viewModel.enableToolbarEditButton(ui.entries.isNotEmpty()) {
                ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
            }
        }
    }

    override fun onStop() {
        saveConfig()
        viewModel.disableToolbarEditButton()
        if (isInitialized) {
            ui.exitMultiSelect()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (isInitialized) {
            ui.removeItemChangedListener()
        }
        super.onDestroy()
    }

    companion object {
        const val TITLE = "title"
        const val LANG = "lang"
        const val DEFAULT_LANG = "zh_CN"
    }
}