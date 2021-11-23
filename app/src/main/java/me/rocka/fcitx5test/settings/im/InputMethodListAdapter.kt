package me.rocka.fcitx5test.settings.im

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.FragmentInputMethodEntryBinding
import me.rocka.fcitx5test.native.Fcitx

class InputMethodListAdapter(private val fcitx: Fcitx) :
    RecyclerView.Adapter<InputMethodListAdapter.ViewHolder>() {
    val entries = fcitx.enabledIme()

    inner class ViewHolder(binding: FragmentInputMethodEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val name = binding.inputMethodName
        val configureButton = binding.inputMethodConfigure
    }

    private fun updateIMState() {
        fcitx.setEnabledIme(entries.map { it.uniqueName }.toTypedArray())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentInputMethodEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = entries[position]
        holder.run {
            name.text = item.name
            holder.configureButton.setOnClickListener {
                it.findNavController().navigate(
                    R.id.action_imListFragment_to_imConfigFragment,
                    bundleOf(InputMethodConfigFragment.ARG_NAME to item.uniqueName)
                )
            }
        }
    }

    override fun getItemCount(): Int = entries.size
}