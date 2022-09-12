package org.fcitx.fcitx5.android.ui.main.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.DynamicListUi
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.config.ConfigType

class ListFragment : Fragment() {

    @Suppress("UNCHECKED_CAST")
    private val descriptor: ConfigDescriptor<*, out List<*>> by lazy {
        requireArguments().get(ARG_DESC) as ConfigDescriptor<*, List<*>>
    }
    private val cfg: RawConfig by lazy {
        requireArguments().get(ARG_CFG) as RawConfig
    }

    private val viewModel: MainViewModel by activityViewModels()

    private val ui: BaseDynamicListUi<*> by lazy {
        when (descriptor) {

            is ConfigDescriptor.ConfigEnumList -> {
                val d = descriptor as ConfigDescriptor.ConfigEnumList
                requireContext().DynamicListUi(
                    BaseDynamicListUi.Mode.ChooseOne { d.entries.toTypedArray() },
                    cfg.subItems?.map { it.value } ?: listOf()
                ) {
                    d.entriesI18n?.get(d.entries.indexOf(it)) ?: it
                }
            }

            is ConfigDescriptor.ConfigList -> {
                val ty = descriptor.type as ConfigType.TyList
                when (ty.subtype) {
                    // does a list of booleans make sense?
                    ConfigType.TyBool -> {
                        requireContext().DynamicListUi(
                            BaseDynamicListUi.Mode.ChooseOne { arrayOf(true, false) },
                            (cfg.subItems?.map { it.value.toBoolean() } ?: listOf()),
                        ) { it.toString() }
                    }
                    ConfigType.TyInt -> {
                        requireContext().DynamicListUi(
                            BaseDynamicListUi.Mode.FreeAdd(
                                "integer",
                                { it.toInt() },
                                { it.toIntOrNull() != null }),
                            (cfg.subItems?.map { it.value.toInt() } ?: listOf()),

                            ) { it.toString() }
                    }
                    ConfigType.TyString -> {
                        requireContext().DynamicListUi(
                            BaseDynamicListUi.Mode.FreeAddString(),
                            (cfg.subItems?.map { it.value } ?: listOf())
                        ) { it }
                    }
                    ConfigType.TyKey -> {
                        object : BaseDynamicListUi<Key>(
                            requireContext(),
                            Mode.FreeAdd(
                                hint = "",
                                converter = { Key.parse(it) }
                            ),
                            (cfg.subItems?.map { Key.parse(it.value) } ?: listOf())
                        ) {
                            override fun showEntry(x: Key) = x.localizedString
                            override fun showEditDialog(
                                title: String,
                                entry: Key?,
                                block: (Key) -> Unit
                            ) {
                                val ui = KeyPreferenceUi(requireContext()).apply {
                                    setKey(entry ?: Key.None)
                                }
                                AlertDialog.Builder(context)
                                    .setTitle(title)
                                    .setView(ui.root)
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        val newKey = ui.lastKey
                                        if (newKey.sym == Key.None.sym) {
                                            entry?.let { removeItem(indexItem(it)) }
                                        } else {
                                            block(ui.lastKey)
                                        }
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show()
                            }
                        }
                    }
                    ConfigType.TyEnum -> error("Impossible!")
                    else -> throw IllegalArgumentException("List of ${ConfigType.pretty(ty.subtype)} is unsupported")
                }
            }
            else -> throw IllegalArgumentException("$descriptor is not a list-like descriptor")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setToolbarTitle(descriptor.description ?: descriptor.name)
        viewModel.disableToolbarSaveButton()
        return ui.root
    }

    override fun onDestroy() {
        val items = Array(ui.entries.size) { RawConfig("$it", ui.entries[it]!!.toString()) }
        parentFragmentManager.setFragmentResult(descriptor.name, bundleOf(descriptor.name to items))
        super.onDestroy()
    }

    companion object {
        const val ARG_DESC = "desc"
        const val ARG_CFG = "cfg"
        val supportedSubtypes = listOf(
            ConfigType.TyEnum,
            ConfigType.TyString,
            ConfigType.TyInt,
            ConfigType.TyBool,
            ConfigType.TyKey
        )
    }

}