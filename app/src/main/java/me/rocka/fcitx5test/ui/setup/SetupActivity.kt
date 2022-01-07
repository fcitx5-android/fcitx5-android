package me.rocka.fcitx5test.ui.setup

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.ActivitySetupBinding
import me.rocka.fcitx5test.ui.setup.SetupPage.Companion.firstUndonePage
import me.rocka.fcitx5test.ui.setup.SetupPage.Companion.isLastPage
import me.rocka.fcitx5test.utils.getCurrentFragment

class SetupActivity : FragmentActivity() {

    private lateinit var viewPager: ViewPager2

    private val viewModel: SetupViewModel by viewModels()

    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prevButton = binding.prevButton.apply {
            text = getString(R.string.prev)
            setOnClickListener { viewPager.currentItem = viewPager.currentItem - 1 }
        }
        nextButton = binding.nextButton.apply {
            setOnClickListener {
                if (viewPager.currentItem != SetupPage.values().size - 1)
                    viewPager.currentItem = viewPager.currentItem + 1
                else finish()
            }
        }
        viewPager = binding.viewpager
        viewPager.adapter = Adapter()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // hide prev button for the first page
                prevButton.visibility = if (position != 0) View.VISIBLE else View.GONE
                nextButton.text =
                    getString(
                        if (position.isLastPage())
                            R.string.done else R.string.next
                    )
                // manually call following observer when page changed
                viewModel.isAllDone.postValue(viewModel.isAllDone.value)
            }
        })
        viewModel.isAllDone.observe(this) { allDone ->
            nextButton.apply {
                // hide next button for the last page when allDone == false
                (allDone || !viewPager.currentItem.isLastPage()).let {
                    visibility = if (it) View.VISIBLE else View.GONE
                }
            }
        }
        // skip to undone page
        firstUndonePage()?.let { viewPager.currentItem = it.ordinal }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        (viewPager.getCurrentFragment(supportFragmentManager) as SetupFragment).sync()
    }

    private inner class Adapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = SetupPage.values().size

        override fun createFragment(position: Int): Fragment =
            SetupFragment(SetupPage.values()[position])

    }
}