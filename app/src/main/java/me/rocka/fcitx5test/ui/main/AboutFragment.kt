package me.rocka.fcitx5test.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.ui.common.SimpleAdapter
import me.rocka.fcitx5test.utils.Const
import splitties.views.dsl.recyclerview.recyclerView

class AboutFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = requireContext().recyclerView {
        layoutManager = LinearLayoutManager(context)
        adapter = SimpleAdapter(
            getString(R.string.current_version) to {
                AlertDialog.Builder(context)
                    .setTitle(R.string.current_version)
                    .setMessage(Const.versionInfo)
                    .show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getString(R.string.about))
        viewModel.disableToolbarSaveButton()
    }
}