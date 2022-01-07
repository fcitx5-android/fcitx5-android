package me.rocka.fcitx5test.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import me.rocka.fcitx5test.databinding.FragmentSetupBinding
import me.rocka.fcitx5test.ui.setup.SetupPage.Companion.isLastPage
import kotlin.properties.Delegates

class SetupFragment(private val page: SetupPage) : Fragment() {

    private val viewModel: SetupViewModel by activityViewModels()

    private lateinit var binding: FragmentSetupBinding

    private var isDone: Boolean by Delegates.observable(page.isDone()) { _, _, new ->
        if (new && page.isLastPage())
            viewModel.isAllDone.value = true
        with(binding) {
            hintText.text = page.getHintText(requireContext())
            actionButton.visibility = if (new) View.GONE else View.VISIBLE
            actionButton.text = page.getButtonText(requireContext())
            actionButton.setOnClickListener { page.getButtonAction(requireContext()) }
            doneText.visibility = if (new) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupBinding.inflate(inflater)
        sync()
        return binding.root
    }

    // called on window focus changed
    fun sync() {
        isDone = page.isDone()
    }

    override fun onResume() {
        super.onResume()
        sync()
    }

}