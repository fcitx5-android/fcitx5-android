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
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.databinding.ActivityLogBinding
import org.fcitx.fcitx5.android.ui.common.LogView
import org.fcitx.fcitx5.android.utils.*
import java.io.OutputStreamWriter

class LogActivity : AppCompatActivity() {

    private lateinit var launcher: ActivityResultLauncher<String>
    private lateinit var logView: LogView

    private fun registerLauncher() {
        launcher = registerForActivityResult(CreateDocument("text/plain")) { uri ->
            lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
                runCatching {
                    if (uri != null)
                        contentResolver.openOutputStream(uri)?.let { OutputStreamWriter(it) }
                    else null
                }.bindOnNotNull { x ->
                    x.use {
                        logView
                            .currentLog
                            .let { log ->
                                runCatching {
                                    it.write("--------- Build Info\n")
                                    it.write("Build Time: ${iso8601UTCDateTime(Const.buildTime)}\n")
                                    it.write("Build Git Hash: ${Const.buildGitHash}\n")
                                    it.write("Build Version Name: ${Const.versionName}\n")
                                    it.write("--------- Device Info\n")
                                    it.write(DeviceInfo.get(this@LogActivity))
                                    it.write(log.toString())
                                }
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
            logView.setLogcat(
                if (CustomActivityOnCrash.getConfigFromIntent(intent) == null) {
                    supportActionBar!!.apply {
                        setDisplayHomeAsUpEnabled(true)
                        setTitle(R.string.real_time_logs)
                    }
                    Logcat()
                } else {
                    supportActionBar!!.setTitle(R.string.crash_logs)
                    clearButton.visibility = View.GONE
                    AlertDialog.Builder(this@LogActivity)
                        .setTitle(R.string.app_crash)
                        .setMessage(R.string.app_crash_message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
                    Logcat(FcitxApplication.getLastPid())
                }
            )
            clearButton.setOnClickListener {
                logView.clear()
            }
            exportButton.setOnClickListener {
                launcher.launch("$packageName-${iso8601UTCDateTime()}.txt")
            }
        }
        registerLauncher()
    }
}