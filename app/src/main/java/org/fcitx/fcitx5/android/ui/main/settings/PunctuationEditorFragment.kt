package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.view.View
import androidx.lifecycle.lifecycleScope
import cn.berberman.girls.utils.either.otherwise
import cn.berberman.girls.utils.either.then
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.core.getPunctuationConfig
import org.fcitx.fcitx5.android.core.savePunctuationConfig
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.str
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
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
        val raw = viewModel.fcitx.getPunctuationConfig(lang)
        val cfg = raw["cfg"]
        val desc = raw["desc"]
        // parse config desc to get description text of the options
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
            viewModel.fcitx.savePunctuationConfig(lang, cfg)
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
                val keyLayout = TextInputLayout(
                    requireContext(),
                    null,
                    R.style.Widget_MaterialComponents_TextInputLayout_FilledBox
                ).apply {
                    hint = keyDesc
                }
                val keyField = TextInputEditText(keyLayout.context).also {
                    keyLayout.apply {
                        add(it, lParams(matchParent))
                    }
                }
                val mappingLayout = TextInputLayout(
                    requireContext(),
                    null,
                    R.style.Widget_MaterialComponents_TextInputLayout_FilledBox
                ).apply {
                    hint = mappingDesc
                }
                val mappingField = TextInputEditText(mappingLayout.context).also {
                    mappingLayout.apply {
                        add(it, lParams(matchParent))
                    }
                }
                val altMappingLayout = TextInputLayout(
                    requireContext(),
                    null,
                    R.style.Widget_MaterialComponents_TextInputLayout_FilledBox
                ).apply {
                    hint = altMappingDesc
                }
                val altMappingField = TextInputEditText(altMappingLayout.context).also {
                    altMappingLayout.apply {
                        add(it, lParams(matchParent))
                    }
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

    override fun onDestroy() {
        saveConfig()
        super.onDestroy()
    }

    companion object {
        const val TITLE = "title"
        const val ENTRIES = "Entries"
        const val KEY = "Key"
        const val MAPPING = "Mapping"
        const val ALT_MAPPING = "AltMapping"
    }
}