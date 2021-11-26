package me.rocka.fcitx5test.settings.addon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.FragmentAddonListEntryBinding
import me.rocka.fcitx5test.native.Fcitx

class AddonListAdapter(private val fcitx: Fcitx) :
    RecyclerView.Adapter<AddonListAdapter.ViewHolder>() {

    inner class ViewHolder(binding: FragmentAddonListEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val enabled: CheckBox = binding.addonEnable
        val addonName: TextView = binding.addonName
        val settingsButton: ImageButton = binding.addonSettings
    }

    private val values = fcitx.addons().apply { sortBy { it.uniqueName } }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentAddonListEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
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
            addonName.text = item.displayName
            enabled.setOnCheckedChangeListener { _, isChecked ->
                values[position] = item.copy(enabled = isChecked)
                updateAddonState()
            }
            // our addon shouldn't be disabled
            enabled.isEnabled = item.uniqueName != "androidfrontend"
            settingsButton.visibility =
                if (item.isConfigurable and item.enabled) View.VISIBLE else View.INVISIBLE
            settingsButton.setOnClickListener {
                it.findNavController().navigate(
                    R.id.action_addonListFragment_to_addonConfigFragment,
                    bundleOf(
                        AddonConfigFragment.ARG_UNIQUE_NAME to item.uniqueName,
                        AddonConfigFragment.ARG_NAME to item.displayName
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = values.size

}