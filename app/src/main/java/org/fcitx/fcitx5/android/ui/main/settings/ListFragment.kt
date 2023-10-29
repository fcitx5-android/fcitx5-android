/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
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
import org.fcitx.fcitx5.android.utils.parcelable

class ListFragment : Fragment() {

    private val descriptor: ConfigDescriptor<*, out List<*>> by lazy {
        requireArguments().parcelable(ARG_DESC)!!
    }
    private val cfg: RawConfig by lazy {
        requireArguments().parcelable(ARG_CFG)!!
    }

    private val viewModel: MainViewModel by activityViewModels()

    private var uiInitialized = false

    private val ui: BaseDynamicListUi<*> by lazy {
        val ctx = requireContext()
        when (descriptor) {
            is ConfigDescriptor.ConfigEnumList -> {
                val d = descriptor as ConfigDescriptor.ConfigEnumList
                val available = d.entries.toSet()
                ctx.DynamicListUi(
                    BaseDynamicListUi.Mode.ChooseOne {
                        (available - entries.toSet()).toTypedArray()
                    },
                    initialEntries = cfg.subItems?.map { it.value } ?: listOf(),
                    enableOrder = true,
                    show = { d.entriesI18n?.get(d.entries.indexOf(it)) ?: it }
                )
            }
            is ConfigDescriptor.ConfigList -> {
                val ty = descriptor.type as ConfigType.TyList
                when (ty.subtype) {
                    // does a list of booleans make sense?
                    ConfigType.TyBool -> {
                        ctx.DynamicListUi(
                            BaseDynamicListUi.Mode.ChooseOne { arrayOf(true, false) },
                            initialEntries = cfg.subItems?.map { it.value.toBoolean() } ?: listOf(),
                            enableOrder = true,
                            show = { it.toString() }
                        )
                    }
                    ConfigType.TyInt -> {
                        ctx.DynamicListUi(
                            BaseDynamicListUi.Mode.FreeAdd(
                                hint = "integer",
                                converter = { it.toInt() },
                                validator = { it.toIntOrNull() != null }
                            ),
                            initialEntries = cfg.subItems?.map { it.value.toInt() } ?: listOf(),
                            enableOrder = true,
                            show = { it.toString() }
                        )
                    }
                    ConfigType.TyString -> {
                        ctx.DynamicListUi(
                            BaseDynamicListUi.Mode.FreeAddString(),
                            initialEntries = cfg.subItems?.map { it.value } ?: listOf(),
                            enableOrder = true,
                            show = { it }
                        )
                    }
                    ConfigType.TyKey -> {
                        object : BaseDynamicListUi<Key>(
                            ctx,
                            Mode.FreeAdd(
                                hint = "",
                                converter = { Key.parse(it) }
                            ),
                            initialEntries = cfg.subItems?.map { Key.parse(it.value) } ?: listOf(),
                            enableOrder = true
                        ) {
                            override fun showEntry(x: Key) = x.localizedString
                            override fun showEditDialog(
                                title: String,
                                entry: Key?,
                                block: (Key) -> Unit
                            ) {
                                val ui = KeyPreferenceUi(ctx).apply { setKey(entry ?: Key.None) }
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
                        }.apply {
                            addTouchCallback()
                        }
                    }
                    ConfigType.TyEnum -> error("Impossible!")
                    else -> throw IllegalArgumentException("List of ${ConfigType.pretty(ty.subtype)} is unsupported")
                }
            }
            else -> throw IllegalArgumentException("$descriptor is not a list-like descriptor")
        }.also {
            it.setViewModel(viewModel)
            uiInitialized = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ui.root

    override fun onStart() {
        super.onStart()
        viewModel.setToolbarTitle(descriptor.description ?: descriptor.name)
        if (uiInitialized) {
            viewModel.enableToolbarEditButton(ui.entries.isNotEmpty()) {
                ui.enterMultiSelect(requireActivity().onBackPressedDispatcher)
            }
        }
    }

    override fun onStop() {
        viewModel.disableToolbarEditButton()
        if (uiInitialized) {
            ui.exitMultiSelect()
        }
        super.onStop()
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