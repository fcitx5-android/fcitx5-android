package me.rocka.fcitx5test.settings.addon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.MainViewModel
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.FragmentAddonListBinding
import me.rocka.fcitx5test.native.Fcitx

class AddonListFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()

    private val fcitx: Fcitx
        get() = viewModel.fcitx

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setToolbarTitle(requireContext().getString(R.string.addons_conf))
        viewModel.disableToolbarSaveButton()
        val binding = FragmentAddonListBinding.inflate(inflater)
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AddonListAdapter(fcitx)
        }
        return binding.root
    }

}