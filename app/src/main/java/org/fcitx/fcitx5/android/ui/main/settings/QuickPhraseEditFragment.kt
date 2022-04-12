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
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.str
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.topPadding

class QuickPhraseEditFragment : ProgressFragment(),
    OnItemChangedListener<QuickPhraseEditFragment.QuickPhraseEntry> {

    data class QuickPhraseEntry(val keyword: String, val phrase: String)

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
        quickPhrase = requireArguments().get(ARG) as QuickPhrase
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(quickPhrase.name)
        val initialEntries = withContext(Dispatchers.IO) {
            quickPhrase.loadData().getOrThrow().map {
                QuickPhraseEntry(it.key, it.value)
            }
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
                val keywordField = view(::TextInputEditText)
                val keywordLayout = view(::TextInputLayout).apply {
                    setHint(R.string.quickphrase_keyword)
                    add(keywordField, lParams(matchParent))
                }
                val phraseField = view(::TextInputEditText)
                val phraseLayout = view(::TextInputLayout).apply {
                    setHint(R.string.quickphrase_phrase)
                    add(phraseField, lParams(matchParent))
                }
                entry?.apply {
                    keywordField.setText(keyword)
                    phraseField.setText(phrase)
                }
                val layout = verticalLayout {
                    topPadding = dp(10)
                    horizontalPadding = dp(20)
                    add(keywordLayout, lParams(matchParent))
                    add(phraseLayout, lParams(matchParent))
                }
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        block(
                            QuickPhraseEntry(
                                keywordField.str,
                                phraseField.str
                            )
                        )
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                    .show()
            }


            override fun showEntry(x: QuickPhraseEntry): String = x.run {
                "$keyword\u2003→\u2003$phrase"
            }


        }
        ui.addOnItemChangedListener(this)
        ui.addTouchCallback()
        resetDustman()
        return ui.root
    }

    override fun onItemAdded(idx: Int, item: QuickPhraseEntry) {
        dustman.addOrUpdate(item.keyword, item)
    }

    override fun onItemRemoved(idx: Int, item: QuickPhraseEntry) {
        dustman.remove(item.keyword)
    }

    override fun onItemUpdated(idx: Int, old: QuickPhraseEntry, new: QuickPhraseEntry) {
        dustman.addOrUpdate(new.keyword, new)
    }

    private fun saveConfig() {
        if (!dustman.dirty)
            return
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            quickPhrase.saveData(
                QuickPhraseData(
                    entries.associate { it.keyword to it.phrase })
            )
            launch(Dispatchers.Main) {
                resetDustman()
                // tell parent that we need to reload
                setFragmentResult(RESULT, bundleOf(RESULT to true))
            }
        }
    }

    private fun resetDustman() {
        dustman.reset((entries.associateBy { it.keyword }))
    }

    override fun onPause() {
        saveConfig()
        super.onPause()
    }

    companion object {
        const val ARG = "quickphrase"
        const val RESULT = "dirty"
    }

}