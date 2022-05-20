package org.fcitx.fcitx5.android.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.Licenses
import org.fcitx.fcitx5.android.ui.common.SimpleAdapter
import org.fcitx.fcitx5.android.utils.applyNavBarInsetsBottomPadding
import splitties.views.dsl.recyclerview.recyclerView

class LicensesFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = requireContext().recyclerView {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = SimpleAdapter()
        applyNavBarInsetsBottomPadding()
        lifecycleScope.launch {
            Licenses.getAll()
                .onSuccess {
                    (adapter as SimpleAdapter).updateItems(it.map { library ->
                        library.libraryName to library.licenseUrl?.let { url ->
                            {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    })
                }
                .onFailure {
                    throw it
                }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(getString(R.string.open_source_licenses))
        viewModel.disableToolbarSaveButton()
    }

}