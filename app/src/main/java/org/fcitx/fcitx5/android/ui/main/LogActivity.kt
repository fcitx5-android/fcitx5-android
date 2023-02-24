package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.databinding.ActivityLogBinding
import org.fcitx.fcitx5.android.ui.main.log.LogView
import org.fcitx.fcitx5.android.utils.*

class LogActivity : AppCompatActivity() {

    private lateinit var launcher: ActivityResultLauncher<String>
    private lateinit var logView: LogView

    private fun registerLauncher() {
        launcher = registerForActivityResult(CreateDocument("text/plain")) { uri ->
            lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
                uri?.runCatching {
                    contentResolver.openOutputStream(this)?.use { stream ->
                        stream.bufferedWriter().use { writer ->
                            writer.write(DeviceInfo.get(this@LogActivity))
                            writer.write(logView.currentLog)
                        }
                    }
                }?.toast(this@LogActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTranslucentSystemBars()
        val binding = ActivityLogBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
                bottomMargin = navBars.bottom
            }
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top
            }
            windowInsets
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        with(binding) {
            setSupportActionBar(toolbar)
            this@LogActivity.logView = logView
            if (intent.hasExtra(FROM_CRASH)) {
                supportActionBar!!.setTitle(R.string.crash_logs)
                clearButton.visibility = View.GONE
                AlertDialog.Builder(this@LogActivity)
                    .setTitle(R.string.app_crash)
                    .setMessage(R.string.app_crash_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                logView.append("--------- Crash stacktrace")
                logView.append(intent.getStringExtra(CRASH_STACK_TRACE) ?: "<empty>")
                logView.setLogcat(Logcat(FcitxApplication.getLastPid()))
            } else {
                supportActionBar!!.apply {
                    setDisplayHomeAsUpEnabled(true)
                    setTitle(R.string.real_time_logs)
                }
                logView.setLogcat(Logcat())
            }
            clearButton.setOnClickListener {
                logView.clear()
            }
            exportButton.setOnClickListener {
                launcher.launch("$packageName-${iso8601UTCDateTime()}.txt")
            }
        }
        registerLauncher()
    }

    companion object {
        const val FROM_CRASH = "from_crash"
        const val CRASH_STACK_TRACE = "crash_stack_trace"
    }
}