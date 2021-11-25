package me.rocka.fcitx5test.settings.im

import android.content.ServiceConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.MainViewModel
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.databinding.FragmentInputMethodListBinding
import me.rocka.fcitx5test.native.Fcitx

class InputMethodListFragment : Fragment() {

    private lateinit var fcitx: Fcitx
    private var connection: ServiceConnection? = null
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setToolbarTitle(requireContext().resources.getString(R.string.input_methods_conf))
        viewModel.disableToolbarSaveButton()
        val binding = FragmentInputMethodListBinding.inflate(inflater)
        binding.list.run {
            layoutManager = LinearLayoutManager(context)
            connection = requireActivity().bindFcitxDaemon {
                fcitx = getFcitxInstance()
                adapter = InputMethodListAdapter(fcitx, binding.fab).also { it.updateFAB() }
                ItemTouchHelper(InputMethodListTouchCallback(adapter as InputMethodListAdapter))
                    .attachToRecyclerView(this@run)
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        connection?.let { requireActivity().unbindService(it) }
        connection = null
        super.onDestroy()
    }
}