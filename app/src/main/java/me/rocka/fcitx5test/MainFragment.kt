package me.rocka.fcitx5test

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.asset.AssetManager
import me.rocka.fcitx5test.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setToolbarTitle(requireContext().getString(R.string.app_name))
        viewModel.disableToolbarSaveButton()
        val binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.settingsList.apply {
            adapter = SettingItemRecyclerViewAdapter(
                context.getString(R.string.open_ime_settings) to {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                },
                context.getString(R.string.global_options) to {
                    findNavController().navigate(R.id.action_mainFragment_to_globalConfigFragment)
                },
                context.getString(R.string.input_methods) to {
                    findNavController().navigate(R.id.action_mainFragment_to_imListFragment)
                },
                context.getString(R.string.addons) to {
                    findNavController().navigate(R.id.action_mainFragment_to_addonListFragment)
                },
                context.getString(R.string.behavior) to {
                    findNavController().navigate(R.id.behaviorSettingsFragment)
                },
                context.getString(R.string.clean_and_sync_assets) to {
                    GlobalScope.launch(Dispatchers.IO) {
                        AssetManager.cleanAndSync()
                        launch(Dispatchers.Main){
                            Toast.makeText(context, "Synced!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        binding.versionInfo.text = Const.versionInfo
        return binding.root
    }
}
