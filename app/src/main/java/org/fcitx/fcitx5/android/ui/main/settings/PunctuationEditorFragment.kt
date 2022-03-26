package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import androidx.lifecycle.lifecycleScope
import cn.berberman.girls.utils.either.otherwise
import cn.berberman.girls.utils.either.then
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.str
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.topPadding
import kotlin.properties.Delegates

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

    lateinit var initialEntries: List<PunctuationMapEntry>

    private var dirty by Delegates.observable(false) { _, old, new ->
        if (old == new) return@observable
        lifecycleScope.launch {
            if (new) {
                viewModel.enableToolbarSaveButton { saveConfig() }
            } else {
                viewModel.disableToolbarSaveButton()
            }
        }
    }

    private suspend fun loadEntries(lang: String = "zh_CN"): List<PunctuationMapEntry> {
        val raw = viewModel.fcitx.getAddonSubConfig("punctuation", "punctuationmap/$lang")
        val cfg = raw["cfg"]
        val desc = raw["desc"]
        ConfigDescriptor.parseTopLevel(desc)
            .otherwise { throw it }
            .then {
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
        val cfg = RawConfig(
            arrayOf(
                RawConfig(
                    ENTRIES,
                    ui.entries.mapIndexed { i, it -> it.toRawConfig(i) }.toTypedArray()
                )
            )
        )
        lifecycleScope.launch {
            viewModel.fcitx.setAddonSubConfig("punctuation", "punctuationmap/$lang", cfg)
        }
    }

    private lateinit var ui: BaseDynamicListUi<PunctuationMapEntry>

    override suspend fun initialize(): View {
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(requireArguments().getString(TITLE)!!)
        initialEntries = loadEntries()
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
                val keyLabel = textView { text = keyDesc }
                val keyInput = editText { }
                val mappingLabel = textView { text = mappingDesc }
                val mappingInput = editText { }
                val altMappingLabel = textView { text = altMappingDesc }
                val altMappingInput = editText { }
                entry?.apply {
                    keyInput.setText(key)
                    mappingInput.setText(mapping)
                    altMappingInput.setText(altMapping)
                }
                val layout = verticalLayout {
                    topPadding = dp(10)
                    horizontalPadding = dp(20)
                    add(keyLabel, lParams { horizontalMargin = dp(4) })
                    add(keyInput, lParams(matchParent))
                    add(mappingLabel, lParams { horizontalMargin = dp(4) })
                    add(mappingInput, lParams(matchParent))
                    add(altMappingLabel, lParams { horizontalMargin = dp(4) })
                    add(altMappingInput, lParams(matchParent))
                }
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        block(
                            PunctuationMapEntry(keyInput.str, mappingInput.str, altMappingInput.str)
                        )
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                    .show()
            }
        }
        return ui.root
    }

    private fun updateDirty() {
        dirty = initialEntries != ui.entries
    }

    override fun onItemAdded(idx: Int, item: PunctuationMapEntry) {
        updateDirty()
    }

    override fun onItemRemoved(idx: Int, item: PunctuationMapEntry) {
        updateDirty()
    }

    override fun onItemUpdated(idx: Int, old: PunctuationMapEntry, new: PunctuationMapEntry) {
        updateDirty()
    }

    companion object {
        const val TITLE = "title"
        const val ENTRIES = "Entries"
        const val KEY = "Key"
        const val MAPPING = "Mapping"
        const val ALT_MAPPING = "AltMapping"
    }
}