package me.rocka.fcitx5test.ui.setup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.ActivitySetupBinding
import me.rocka.fcitx5test.ui.setup.SetupPage.Companion.firstUndonePage
import me.rocka.fcitx5test.ui.setup.SetupPage.Companion.isLastPage
import me.rocka.fcitx5test.utils.getCurrentFragment
import splitties.systemservices.notificationManager

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
                // manually call following observer when page changed
                // intentionally before changing the text of nextButton
                viewModel.isAllDone.value = viewModel.isAllDone.value
                // hide prev button for the first page
                prevButton.visibility = if (position != 0) View.VISIBLE else View.GONE
                nextButton.text =
                    getString(
                        if (position.isLastPage())
                            R.string.done else R.string.next
                    )
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
        shown = true
        createNotificationChannel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        (viewPager.getCurrentFragment(supportFragmentManager) as SetupFragment).sync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getText(R.string.setup_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CHANNEL_ID }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onPause() {
        if (SetupPage.hasUndonePage())
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_keyboard_24)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.setup_keyboard))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, javaClass),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build()
                .let { notificationManager.notify(NOTIFY_ID, it) }
        super.onPause()
    }

    override fun onResume() {
        notificationManager.cancel(NOTIFY_ID)
        super.onResume()
    }

    private inner class Adapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = SetupPage.values().size

        override fun createFragment(position: Int): Fragment =
            SetupFragment().apply {
                arguments = bundleOf("page" to SetupPage.values()[position])
            }
    }

    companion object {
        private var shown = false
        private const val CHANNEL_ID = "setup"
        private const val NOTIFY_ID = 233
        fun shouldShowUp() = !shown && SetupPage.hasUndonePage()
    }
}