/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.reloadPinyinCustomPhrase
import org.fcitx.fcitx5.android.data.pinyin.CustomPhraseManager
import org.fcitx.fcitx5.android.data.pinyin.customphrase.PinyinCustomPhrase
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.materialTextInput
import org.fcitx.fcitx5.android.utils.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.setPaddingDp
import kotlin.math.absoluteValue
import kotlin.math.min

class PinyinCustomPhraseFragment : Fragment(), OnItemChangedListener<PinyinCustomPhrase> {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var ui: BaseDynamicListUi<PinyinCustomPhrase>

    private val dustman = NaiveDustman<PinyinCustomPhrase>()

    private val initialItems = CustomPhraseManager.load() ?: emptyArray()

    private var keyLabel = KEY
    private var orderLabel = ORDER
    private var phraseLabel = PHRASE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lifecycleScope.launch {
            viewModel.fcitx.runOnReady {
                keyLabel = translate(KEY, CHINESE_ADDONS_DOMAIN)
                orderLabel = translate(ORDER, CHINESE_ADDONS_DOMAIN)
                phraseLabel = translate(PHRASE, CHINESE_ADDONS_DOMAIN)
            }
        }
        val initialEntries = initialItems.toList()
        ui = object : BaseDynamicListUi<PinyinCustomPhrase>(
            requireContext(),
            Mode.FreeAdd("", converter = { PinyinCustomPhrase("", 1, "") }),
            initialItems.toList(),
            enableOrder = true,
            initCheckBox = { entry ->
                setOnCheckedChangeListener(null)
                isChecked = entry.enabled
                setOnCheckedChangeListener { _, checked ->
                    ui.updateItem(ui.indexItem(entry), entry.copyEnabled(checked))
                }
            }
        ) {
            override fun showEntry(x: PinyinCustomPhrase): String {
                val s = x.serialize()
                val firstLF = s.indexOf('\n')
                val endIndex = min(if (firstLF > 0) firstLF else s.length, 20)
                return if (endIndex == s.length) {
                    s
                } else {
                    s.substring(0, endIndex) + "…"
                }
            }

            override fun showEditDialog(
                title: String,
                entry: PinyinCustomPhrase?,
                block: (PinyinCustomPhrase) -> Unit
            ) {
                val (keyLayout, keyField) = materialTextInput {
                    hint = keyLabel
                }
                keyField.apply {
                    isSingleLine = true
                    filters = arrayOf(
                        InputFilter { source, _, _, _, _, _ ->
                            source.filter { it.code in 'A'.code..'Z'.code || it.code in 'a'.code..'z'.code }
                        }
                    )
                    imeOptions = EditorInfo.IME_ACTION_NEXT
                    inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                }
                val (orderLayout, orderField) = materialTextInput {
                    hint = orderLabel
                }
                orderField.apply {
                    inputType =
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    imeOptions = EditorInfo.IME_ACTION_NEXT
                }
                val (phraseLayout, phraseField) = materialTextInput {
                    hint = phraseLabel
                }
                entry?.apply {
                    keyField.setText(key)
                    orderField.setText(order.absoluteValue.toString())
                    phraseField.setText(value)
                }
                val layout = verticalLayout {
                    setPaddingDp(20, 10, 20, 0)
                    add(keyLayout, lParams(matchParent))
                    add(orderLayout, lParams(matchParent))
                    add(phraseLayout, lParams(matchParent))
                }
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        block(
                            PinyinCustomPhrase(
                                keyField.str,
                                orderField.str.toIntOrNull() ?: 1,
                                phraseField.str
                            )
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
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

    override fun onItemAdded(idx: Int, item: PinyinCustomPhrase) {
        dustman.addOrUpdate(item.serialize(), item)
    }

    override fun onItemRemoved(idx: Int, item: PinyinCustomPhrase) {
        dustman.remove(item.serialize())
    }

    override fun onItemRemovedBatch(indexed: List<Pair<Int, PinyinCustomPhrase>>) {
        batchRemove(indexed)
    }

    override fun onItemUpdated(idx: Int, old: PinyinCustomPhrase, new: PinyinCustomPhrase) {
        dustman.remove(old.serialize())
        dustman.addOrUpdate(new.serialize(), new)
    }

    private fun saveConfig() {
        if (!dustman.dirty) return
        resetDustman()
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            CustomPhraseManager.save(ui.entries.toTypedArray())
            viewModel.fcitx.runOnReady {
                reloadPinyinCustomPhrase()
            }
        }
    }

    private fun resetDustman() {
        dustman.reset(ui.entries.associateBy { it.serialize() })
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val title = viewModel.fcitx.runOnReady {
                translate(MANAGE_CUSTOM_PHRASE, CHINESE_ADDONS_DOMAIN)
            }
            viewModel.setToolbarTitle(title)
        }
        viewModel.enableToolbarEditButton(ui.entries.isNotEmpty()) {
            ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
        }
    }

    override fun onStop() {
        saveConfig()
        ui.exitMultiSelect()
        viewModel.disableToolbarEditButton()
        super.onStop()
    }

    override fun onDestroy() {
        ui.removeItemChangedListener()
        super.onDestroy()
    }

    companion object {
        const val CHINESE_ADDONS_DOMAIN = "fcitx5-chinese-addons"
        const val KEY = "Key"
        const val ORDER = "Order"
        const val PHRASE = "Phrase"
        const val MANAGE_CUSTOM_PHRASE = "Manage Custom Phrase"
    }

}
