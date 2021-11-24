package me.rocka.fcitx5test.settings.im

import android.content.ServiceConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.MainViewModel
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.databinding.FragmentInputMethodListBinding
import me.rocka.fcitx5test.native.Fcitx

/**
 * A fragment representing a list of Items.
 */
class InputMethodListFragment : Fragment() {

    private lateinit var fcitx: Fcitx
    private var connection: ServiceConnection? = null
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setToolbarTitle("Input Methods")
        viewModel.disableToolbarSaveButton()
        val binding = FragmentInputMethodListBinding.inflate(inflater)
        binding.list.run {
            layoutManager = LinearLayoutManager(context)
            connection = requireActivity().bindFcitxDaemon {
                fcitx = getFcitxInstance()
                adapter = InputMethodListAdapter(fcitx)
            }
        }
        return binding.root
    }

    override fun onDestroy() {
        connection?.let { requireActivity().unbindService(it) }
        super.onDestroy()
    }
}