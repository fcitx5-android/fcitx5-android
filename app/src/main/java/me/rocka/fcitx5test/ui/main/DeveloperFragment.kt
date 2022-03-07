package me.rocka.fcitx5test.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.DataManager
import me.rocka.fcitx5test.ui.common.SimpleAdapter
import splitties.views.dsl.recyclerview.recyclerView

class DeveloperFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = requireContext().recyclerView {
        layoutManager = LinearLayoutManager(context)
        adapter = SimpleAdapter(
            getString(R.string.real_time_logs) to {
                findNavController().navigate(R.id.action_developerFragment_to_logFragment)
            },
            context.getString(R.string.delete_and_sync_data) to {
                AlertDialog.Builder(context)
                    .setTitle(R.string.delete_and_sync_data)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            DataManager.deleteAndSync()
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    getString(R.string.synced),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getString(R.string.developer))
        viewModel.disableToolbarSaveButton()
    }
}