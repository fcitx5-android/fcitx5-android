package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.common.SimpleAdapter
import splitties.views.dsl.recyclerview.recyclerView

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return requireContext().recyclerView {
            layoutManager = LinearLayoutManager(context)
            adapter = SimpleAdapter(
                getString(R.string.global_options) to {
                    findNavController().navigate(R.id.action_mainFragment_to_globalConfigFragment)
                },
                getString(R.string.input_methods) to {
                    findNavController().navigate(R.id.action_mainFragment_to_imListFragment)
                },
                getString(R.string.addons) to {
                    findNavController().navigate(R.id.action_mainFragment_to_addonListFragment)
                },
                getString(R.string.behavior) to {
                    findNavController().navigate(R.id.action_mainFragment_to_behaviorSettingsFragment)
                },
                getString(R.string.developer) to {
                    findNavController().navigate(R.id.action_mainFragment_to_developerFragment)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.app_name))
        viewModel.disableToolbarSaveButton()
        viewModel.enableAboutButton()
    }

    override fun onPause() {
        viewModel.disableAboutButton()
        super.onPause()
    }
}