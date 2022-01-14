package me.rocka.fcitx5test.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import me.rocka.fcitx5test.native.RawConfig
import me.rocka.fcitx5test.ui.common.BaseDynamicListUi
import me.rocka.fcitx5test.ui.main.MainViewModel
import me.rocka.fcitx5test.utils.config.ConfigDescriptor
import me.rocka.fcitx5test.utils.config.ConfigType

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
                object : BaseDynamicListUi<String>(
                    requireContext(),
                    Mode.ChooseOne { d.entries.toTypedArray() },
                    cfg.subItems?.map { it.value } ?: listOf()
                ) {
                    override fun showEntry(x: String): String =
                        d.entriesI18n?.get(d.entries.indexOf(x)) ?: x
                }
            }

            is ConfigDescriptor.ConfigList -> {
                val ty = descriptor.type as ConfigType.TyList
                when (ty.subtype) {
                    // does a list of booleans make sense?
                    ConfigType.TyBool -> {
                        object : BaseDynamicListUi<Boolean>(
                            requireContext(),
                            Mode.ChooseOne { arrayOf(true, false) },
                            (cfg.subItems?.map { it.value.toBoolean() } ?: listOf())
                        ) {
                            override fun showEntry(x: Boolean): String = x.toString()
                        }
                    }
                    ConfigType.TyInt -> {
                        object : BaseDynamicListUi<Int>(
                            requireContext(),
                            Mode.FreeAdd("integer", { it.toInt() }, { it.toIntOrNull() != null }),
                            (cfg.subItems?.map { it.value.toInt() } ?: listOf())
                        ) {
                            override fun showEntry(x: Int): String = x.toString()
                        }
                    }
                    ConfigType.TyString -> {
                        object : BaseDynamicListUi<String>(
                            requireContext(),
                            Mode.FreeAddString(),
                            (cfg.subItems?.map { it.value } ?: listOf())
                        ) {
                            override fun showEntry(x: String): String = x
                        }
                    }
                    ConfigType.TyEnum -> throw IllegalAccessException("Impossible!")
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
        cfg.subItems = ui.entries.mapIndexed { index, it ->
            RawConfig(index.toString(), it.toString())
        }.toTypedArray()
        parentFragmentManager.setFragmentResult(descriptor.name, bundleOf(descriptor.name to cfg))
        super.onDestroy()
    }

    companion object {
        const val ARG_DESC = "desc"
        const val ARG_CFG = "cfg"
        val supportedSubtypes =
            listOf(ConfigType.TyEnum, ConfigType.TyString, ConfigType.TyInt, ConfigType.TyBool)
    }

}