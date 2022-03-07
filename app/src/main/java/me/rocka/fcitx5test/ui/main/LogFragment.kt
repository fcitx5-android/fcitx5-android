package me.rocka.fcitx5test.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.ui.common.LogView
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout

class LogFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = requireContext().verticalLayout {
        add(LogView(requireContext()), lParams {
            height = matchParent
            width = matchParent
            setPadding(dp(10))
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getString(R.string.real_time_logs))
        viewModel.disableToolbarSaveButton()
    }

}