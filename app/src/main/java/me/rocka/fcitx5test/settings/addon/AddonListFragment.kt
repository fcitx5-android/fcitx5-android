package me.rocka.fcitx5test.settings.addon

import android.content.ServiceConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.bindFcitxDaemon
import me.rocka.fcitx5test.databinding.FragmentAddonListBinding
import me.rocka.fcitx5test.native.Fcitx

class AddonListFragment : Fragment() {
    private lateinit var fcitx: Fcitx
    private var connection: ServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connection = requireActivity().bindFcitxDaemon {
            fcitx = it.getFcitxInstance()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAddonListBinding.inflate(inflater)
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            connection = requireActivity().bindFcitxDaemon {
                fcitx = it.getFcitxInstance()
                adapter = AddonListAdapter(fcitx)
            }
        }
        return binding.root
    }

    override fun onDestroy() {
        connection?.let { requireActivity().unbindService(it) }
        super.onDestroy()
    }

}