package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import androidx.lifecycle.lifecycleScope
import arrow.core.redeem
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.core.getPunctuationConfig
import org.fcitx.fcitx5.android.daemon.launchOnFcitxReady
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager
import org.fcitx.fcitx5.android.data.punctuation.PunctuationMapEntry
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.str
import splitties.views.dsl.core.*
import splitties.views.dsl.material.addInput
import splitties.views.setPaddingDp

class PunctuationEditorFragment : ProgressFragment(), OnItemChangedListener<PunctuationMapEntry> {

    private lateinit var lang: String
    private lateinit var keyDesc: String
    private lateinit var mappingDesc: String
    private lateinit var altMappingDesc: String

    private val dustman = NaiveDustman<PunctuationMapEntry>().apply {
        onDirty = {
            viewModel.enableToolbarSaveButton { saveConfig() }
        }
        onClean = {
            viewModel.disableToolbarSaveButton()
        }
    }

    private val entries
        get() = ui.entries

    private fun findDesc(raw: RawConfig) {
        val desc = raw["desc"]
        // parse config desc to get description text of the options
        ConfigDescriptor.parseTopLevel(desc)
            .redeem({ throw it }) {
                it.customTypes.first().values.forEach { descriptor ->
                    when (descriptor.name) {
                        PunctuationManager.KEY ->
                            keyDesc = descriptor.description ?: descriptor.name
                        PunctuationManager.MAPPING ->
                            mappingDesc = descriptor.description ?: descriptor.name
                        PunctuationManager.ALT_MAPPING ->
                            altMappingDesc = descriptor.description ?: descriptor.name
                    }
                }
            }
    }

    private fun saveConfig() {
        if (!dustman.dirty)
            return
        resetDustman()
        lifecycleScope.launchOnFcitxReady(fcitx) {
            PunctuationManager.save(it, lang, ui.entries)
        }
    }

    private lateinit var ui: BaseDynamicListUi<PunctuationMapEntry>

    private fun resetDustman() {
        dustman.reset((entries.associateBy { it.key }))
    }

    override suspend fun initialize(): View {
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(requireArguments().getString(TITLE)!!)
        lang = requireArguments().getString(LANG, DEFAULT_LANG)
        val raw = fcitx.runOnReady { getPunctuationConfig(lang) }
        findDesc(raw)
        val initialEntries = PunctuationManager.parseRawConfig(raw)
        ui = object : BaseDynamicListUi<PunctuationMapEntry>(
            requireContext(),
            Mode.FreeAdd(hint = "", converter = { PunctuationMapEntry(it, "", "") }),
            initialEntries
        ) {
            init {
                addTouchCallback()
                addOnItemChangedListener(this@PunctuationEditorFragment)
            }

            override fun showEntry(x: PunctuationMapEntry) = x.run {
                "$key\u2003→\u2003$mapping $altMapping"
            }

            override fun showEditDialog(
                title: String,
                entry: PunctuationMapEntry?,
                block: (PunctuationMapEntry) -> Unit
            ) {
                val keyField: TextInputEditText
                val keyLayout = view(::TextInputLayout) {
                    hint = keyDesc
                    keyField = addInput(View.NO_ID)
                }
                val mappingField: TextInputEditText
                val mappingLayout = view(::TextInputLayout) {
                    hint = mappingDesc
                    mappingField = addInput(View.NO_ID)
                }
                val altMappingField: TextInputEditText
                val altMappingLayout = view(::TextInputLayout) {
                    hint = altMappingDesc
                    altMappingField = addInput(View.NO_ID)
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
        return ui.root
    }

    override fun onItemAdded(idx: Int, item: PunctuationMapEntry) {
        dustman.addOrUpdate(item.key, item)
    }

    override fun onItemRemoved(idx: Int, item: PunctuationMapEntry) {
        dustman.remove(item.key)
    }

    override fun onItemUpdated(idx: Int, old: PunctuationMapEntry, new: PunctuationMapEntry) {
        dustman.addOrUpdate(new.key, new)
    }

    override fun onPause() {
        saveConfig()
        super.onPause()
    }

    companion object {
        const val TITLE = "title"
        const val LANG = "lang"
        const val DEFAULT_LANG = "zh_CN"
    }
}