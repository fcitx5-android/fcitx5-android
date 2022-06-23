package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import androidx.lifecycle.lifecycleScope
import arrow.core.redeem
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.core.getPunctuationConfig
import org.fcitx.fcitx5.android.core.savePunctuationConfig
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.str
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.topPadding

class PunctuationEditorFragment : ProgressFragment(),
    OnItemChangedListener<PunctuationEditorFragment.PunctuationMapEntry> {

    data class PunctuationMapEntry(val key: String, val mapping: String, val altMapping: String) {
        constructor(it: RawConfig) : this(it[KEY].value, it[MAPPING].value, it[ALT_MAPPING].value)

        fun toRawConfig(idx: Int) = RawConfig(
            idx.toString(), arrayOf(
                RawConfig(KEY, key),
                RawConfig(MAPPING, mapping),
                RawConfig(ALT_MAPPING, altMapping),
            )
        )
    }

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

    private suspend fun loadEntries(lang: String = "zh_CN"): List<PunctuationMapEntry> {
        val raw = viewModel.fcitx.getPunctuationConfig(lang)
        val cfg = raw["cfg"]
        val desc = raw["desc"]
        // parse config desc to get description text of the options
        ConfigDescriptor.parseTopLevel(desc)
            .redeem({ throw it }) {
                it.customTypes.first().values.forEach { descriptor ->
                    when (descriptor.name) {
                        KEY -> keyDesc = descriptor.description ?: descriptor.name
                        MAPPING -> mappingDesc = descriptor.description ?: descriptor.name
                        ALT_MAPPING -> altMappingDesc = descriptor.description ?: descriptor.name
                    }
                }
            }
        return cfg[ENTRIES].subItems?.map { PunctuationMapEntry(it) } ?: listOf()
    }

    private fun saveConfig(lang: String = "zh_CN") {
        if (!dustman.dirty)
            return
        val cfg = RawConfig(
            arrayOf(
                RawConfig(
                    ENTRIES,
                    ui.entries.mapIndexed { i, it -> it.toRawConfig(i) }.toTypedArray()
                )
            )
        )
        lifecycleScope.launch {
            viewModel.fcitx.savePunctuationConfig(lang, cfg)
            resetDustman()
        }
    }

    private lateinit var ui: BaseDynamicListUi<PunctuationMapEntry>

    private fun resetDustman() {
        dustman.reset((entries.associateBy { it.key }))
    }

    override suspend fun initialize(): View {
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(requireArguments().getString(TITLE)!!)
        val initialEntries = loadEntries()
        ui = object : BaseDynamicListUi<PunctuationMapEntry>(
            requireContext(),
            Mode.FreeAdd(
                hint = "",
                converter = { PunctuationMapEntry(it, "", "") }
            ),
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
                val keyField = view(::TextInputEditText)
                val keyLayout = view(::TextInputLayout) {
                    hint = keyDesc
                    add(keyField, lParams(matchParent))
                }
                val mappingField = view(::TextInputEditText)
                val mappingLayout = view(::TextInputLayout) {
                    hint = mappingDesc
                    add(mappingField, lParams(matchParent))
                }
                val altMappingField = view(::TextInputEditText)
                val altMappingLayout = view(::TextInputLayout) {
                    hint = altMappingDesc
                    add(altMappingField, lParams(matchParent))
                }
                entry?.apply {
                    keyField.setText(key)
                    mappingField.setText(mapping)
                    altMappingField.setText(altMapping)
                }
                val layout = verticalLayout {
                    topPadding = dp(10)
                    horizontalPadding = dp(20)
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
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
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
        const val ENTRIES = "Entries"
        const val KEY = "Key"
        const val MAPPING = "Mapping"
        const val ALT_MAPPING = "AltMapping"
    }
}