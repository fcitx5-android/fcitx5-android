package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
import org.fcitx.fcitx5.android.utils.serializable
import org.fcitx.fcitx5.android.utils.str
import splitties.views.dsl.core.*
import splitties.views.dsl.material.addInput
import splitties.views.setPaddingDp

class QuickPhraseEditFragment : ProgressFragment(), OnItemChangedListener<QuickPhraseEntry> {

    private lateinit var ui: BaseDynamicListUi<QuickPhraseEntry>

    private val entries: List<QuickPhraseEntry>
        get() = ui.entries

    private lateinit var quickPhrase: QuickPhrase

    private val dustman = NaiveDustman<QuickPhraseEntry>().apply {
        onDirty = {
            viewModel.enableToolbarSaveButton { saveConfig() }
        }
        onClean = {
            viewModel.disableToolbarSaveButton()
        }
    }

    override suspend fun initialize(): View {
        quickPhrase = requireArguments().serializable(ARG)!!
        val initialEntries = withContext(Dispatchers.IO) {
            quickPhrase.loadData().getOrThrow()
        }
        ui = object : BaseDynamicListUi<QuickPhraseEntry>(
            requireContext(),
            Mode.FreeAdd("", converter = { QuickPhraseEntry("", "") }),
            initialEntries,
        ) {
            override fun showEditDialog(
                title: String,
                entry: QuickPhraseEntry?,
                block: (QuickPhraseEntry) -> Unit
            ) {
                val keywordField: TextInputEditText
                val keywordLayout = view(::TextInputLayout).apply {
                    setHint(R.string.quickphrase_keyword)
                    keywordField = addInput(View.NO_ID)
                }
                val phraseField: TextInputEditText
                val phraseLayout = view(::TextInputLayout).apply {
                    setHint(R.string.quickphrase_phrase)
                    phraseField = addInput(View.NO_ID)
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
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        block(QuickPhraseEntry(keywordField.str, phraseField.str))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            override fun showEntry(x: QuickPhraseEntry): String = x.run {
                "$keyword\u2003→\u2003$phrase"
            }
        }
        ui.addOnItemChangedListener(this)
        ui.addTouchCallback()
        resetDustman()
        viewModel.enableToolbarEditButton {
            ui.enterMultiSelect(
                requireActivity().onBackPressedDispatcher,
                viewModel
            )
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
        if (!dustman.dirty)
            return
        resetDustman()
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            quickPhrase.saveData(
                QuickPhraseData(entries)
            )
            launch(Dispatchers.Main) {
                // tell parent that we need to reload
                setFragmentResult(RESULT, bundleOf(RESULT to true))
            }
        }
    }

    private fun resetDustman() {
        dustman.reset(entries.associateBy { it.serialize() })
    }

    override fun onResume() {
        super.onResume()
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(quickPhrase.name)

        if (::ui.isInitialized)
            viewModel.enableToolbarEditButton {
                ui.enterMultiSelect(
                    requireActivity().onBackPressedDispatcher,
                    viewModel
                )
            }
    }

    override fun onPause() {
        saveConfig()
        ui.exitMultiSelect(viewModel)
        viewModel.disableToolbarEditButton()
        super.onPause()
    }

    companion object {
        const val ARG = "quickphrase"
        const val RESULT = "dirty"
    }

}