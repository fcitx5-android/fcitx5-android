package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.databinding.ActivityLogBinding
import org.fcitx.fcitx5.android.ui.common.LogView
import org.fcitx.fcitx5.android.utils.DeviceInfo
import org.fcitx.fcitx5.android.utils.Logcat
import org.fcitx.fcitx5.android.utils.formatDateTime
import java.io.OutputStreamWriter

class LogActivity : AppCompatActivity() {

    private lateinit var launcher: ActivityResultLauncher<String>
    private lateinit var logView: LogView

    private fun registerLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(it)
                        ?.let { OutputStreamWriter(it) }
                }.getOrNull()?.use {
                    logView
                        .currentLog
                        .let { log ->
                            runCatching {
                                it.write("--------- Device Info\n")
                                it.write(DeviceInfo.get(this@LogActivity))
                                it.write(log.toString())
                            }
                                .onSuccess {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@LogActivity,
                                            R.string.done,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }.onFailure {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@LogActivity,
                                            it.message,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }

                                }
                        }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            setSupportActionBar(toolbar)
            this@LogActivity.logView = logView
            logView.setLogcat(
                if (intent.hasExtra(NOT_CRASH)) {
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
                launcher.launch("$packageName-${formatDateTime()}.txt")
            }
        }
        registerLauncher()
    }

    companion object {
        const val NOT_CRASH = "not_crash"
    }
}