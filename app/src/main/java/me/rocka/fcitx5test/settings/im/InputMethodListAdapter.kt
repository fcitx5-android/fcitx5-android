package me.rocka.fcitx5test.settings.im

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.FragmentInputMethodEntryBinding
import me.rocka.fcitx5test.native.Fcitx
import java.util.*

class InputMethodListAdapter(private val fcitx: Fcitx, val fab: FloatingActionButton) :
    RecyclerView.Adapter<InputMethodListAdapter.ViewHolder>() {

    private val entries = fcitx.enabledIme().toMutableList()

    val unEnabledIme
        get() = fcitx.availableIme().toSet() - entries.toSet()

    fun updateFAB() {
        val unEnabled = unEnabledIme.toList()
        if (unEnabled.isEmpty())
            fab.visibility = View.GONE
        else {
            fab.visibility = View.VISIBLE
            fab.setOnClickListener {
                val items = unEnabled.map { it.name }.toTypedArray()
                AlertDialog.Builder(fab.context)
                    .setTitle(R.string.input_methods)
                    .setItems(items) { _, which ->
                        entries.add(unEnabled[which])
                        updateIMState()
                        notifyItemInserted(entries.size - 1)
                        updateFAB()
                        Snackbar.make(fab, "${unEnabled[which].name} added", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                    .show()
            }
        }
    }

    fun removeItem(item: Int) {
        val removed = entries.removeAt(item)
        updateIMState()
        notifyItemRemoved(item)
        updateFAB()
        Snackbar.make(fab, "${removed.name} removed", Snackbar.LENGTH_SHORT)
            .show()
    }

    fun swapItem(src: Int, dest: Int) {
        Collections.swap(entries, src, dest)
        updateIMState()
        notifyItemMoved(src, dest)
        updateFAB()
    }

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
                    bundleOf(
                        InputMethodConfigFragment.ARG_UNIQUE_NAME to item.uniqueName,
                        InputMethodConfigFragment.ARG_NAME to item.name
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = entries.size
}