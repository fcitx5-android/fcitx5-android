/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseData
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseEntry
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.materialTextInput
import org.fcitx.fcitx5.android.utils.serializable
import org.fcitx.fcitx5.android.utils.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.setPaddingDp

class QuickPhraseEditFragment : ProgressFragment(), OnItemChangedListener<QuickPhraseEntry> {

    private lateinit var ui: BaseDynamicListUi<QuickPhraseEntry>

    private lateinit var quickPhrase: QuickPhrase

    private val dustman = NaiveDustman<QuickPhraseEntry>()

    override suspend fun initialize(): View {
        quickPhrase = requireArguments().serializable(ARG)!!
        val initialEntries = withContext(Dispatchers.IO) {
            quickPhrase.loadData()
        }
        ui = object : BaseDynamicListUi<QuickPhraseEntry>(
            requireContext(),
            Mode.FreeAdd("", converter = { QuickPhraseEntry("", "") }),
            initialEntries,
        ) {
            override fun showEntry(x: QuickPhraseEntry): String = x.run {
                "$keyword → ${phrase.replace("\n", "\\n")}"
            }

            override fun showEditDialog(
                title: String,
                entry: QuickPhraseEntry?,
                block: (QuickPhraseEntry) -> Unit
            ) {
                val (keywordLayout, keywordField) = materialTextInput {
                    setHint(R.string.quickphrase_keyword)
                }
                keywordField.apply {
                    isSingleLine = true
                    imeOptions = EditorInfo.IME_ACTION_NEXT
                }
                val (phraseLayout, phraseField) = materialTextInput {
                    setHint(R.string.quickphrase_phrase)
                }
                entry?.apply {
                    keywordField.setText(keyword)
                    phraseField.setText(phrase)
                }
                val layout = verticalLayout {
                    setPaddingDp(20, 10, 20, 0)
                    add(keywordLayout, lParams(matchParent))
                    add(phraseLayout, lParams(matchParent))
                }
                val dialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                // change OnClickListener after dialog is shown,
                // so we can control when to dismiss the dialog
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener onClick@{
                    val keyword = keywordField.str.trim()
                    // "keyword" cannot contain any black characters
                    if (keyword.isBlank()) {
                        keywordField.error = getString(
                            R.string._cannot_be_empty,
                            getString(R.string.quickphrase_keyword)
                        )
                        keywordField.requestFocus()
                        return@onClick
                    } else {
                        keywordField.error = null
                    }
                    // "phrase" may contain blank characters
                    val phrase = phraseField.str
                    if (phrase.isEmpty()) {
                        phraseField.error = getString(
                            R.string._cannot_be_empty,
                            getString(R.string.quickphrase_phrase)
                        )
                        phraseField.requestFocus()
                        return@onClick
                    } else {
                        phraseField.error = null
                    }
                    block(QuickPhraseEntry(keyword, phraseField.str))
                    dialog.dismiss()
                }
            }
        }
        ui.addOnItemChangedListener(this)
        ui.addTouchCallback()
        resetDustman()
        ui.setViewModel(viewModel)
        viewModel.enableToolbarEditButton(initialEntries.isNotEmpty()) {
            ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
        }
        return ui.root
    }

    // The dustman didn't work well in this case,
    // as one key may map to many phrases

    override fun onItemAdded(idx: Int, item: QuickPhraseEntry) {
        dustman.addOrUpdate(item.serialize(), item)
    }

    override fun onItemRemoved(idx: Int, item: QuickPhraseEntry) {
        dustman.remove(item.serialize())
    }

    override fun onItemRemovedBatch(indexed: List<Pair<Int, QuickPhraseEntry>>) {
        batchRemove(indexed)
    }

    override fun onItemUpdated(idx: Int, old: QuickPhraseEntry, new: QuickPhraseEntry) {
        dustman.addOrUpdate(new.serialize(), new)
    }

    private fun saveConfig() {
        if (!dustman.dirty) return
        resetDustman()
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            quickPhrase.saveData(QuickPhraseData(ui.entries))
            launch(Dispatchers.Main) {
                // tell parent that we need to reload
                parentFragmentManager.setFragmentResult(RESULT, bundleOf(RESULT to true))
            }
        }
    }

    private fun resetDustman() {
        dustman.reset(ui.entries.associateBy { it.serialize() })
    }

    override fun onStart() {
        super.onStart()
        viewModel.setToolbarTitle(quickPhrase.name)
        if (isInitialized) {
            viewModel.enableToolbarEditButton(ui.entries.isNotEmpty()) {
                ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
            }
        }
    }

    override fun onStop() {
        saveConfig()
        if (isInitialized) {
            ui.exitMultiSelect()
        }
        viewModel.disableToolbarEditButton()
        super.onStop()
    }

    override fun onDestroy() {
        if (isInitialized) {
            ui.removeItemChangedListener()
        }
        super.onDestroy()
    }

    companion object {
        const val ARG = "quickphrase"
        const val RESULT = "dirty"
    }

}