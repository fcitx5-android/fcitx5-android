package me.rocka.fcitx5test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.databinding.FragmentAddonListEntryBinding
import me.rocka.fcitx5test.native.Fcitx
import me.rocka.fcitx5test.settings.addon.AddonConfigFragment

class MyAddonRecyclerViewAdapter(
    private val fcitx: Fcitx
) : RecyclerView.Adapter<MyAddonRecyclerViewAdapter.ViewHolder>() {

    private val values = fcitx.addons()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentAddonListEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    private fun updateAddonState() {
        with(values) {
            val ids = map { it.uniqueName }.toTypedArray()
            val state = map { it.enabled }.toBooleanArray()
            fcitx.setAddonState(ids, state)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        with(holder) {
            enabled.isChecked = item.enabled
            enabled.setOnCheckedChangeListener { _, isChecked ->
                values[position] = item.copy(enabled = isChecked)
                updateAddonState()
            }
            settingsButton.visibility = if (item.enabled) View.VISIBLE else View.INVISIBLE
            settingsButton.setOnClickListener {
                it.findNavController().navigate(
                    R.id.action_addonListFragment_to_addonConfigFragment,
                    bundleOf(AddonConfigFragment.ARG_NAME to item.name)
                )
            }
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentAddonListEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val enabled: CheckBox = binding.addonEnable
        val settingsButton: Button = binding.addonSettings
    }

}