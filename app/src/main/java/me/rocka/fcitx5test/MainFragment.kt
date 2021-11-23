package me.rocka.fcitx5test

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.rocka.fcitx5test.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.openImeSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        binding.globalOptions.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_globalConfigFragment)
        }
        binding.inputMethods.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_imListFragment)
        }
        binding.addons.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_addonListFragment)
        }
        binding.versionInfo.text = Const.versionInfo
        return binding.root
    }
}