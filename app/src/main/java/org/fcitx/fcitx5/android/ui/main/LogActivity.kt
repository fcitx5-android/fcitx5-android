package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.databinding.ActivityLogBinding
import org.fcitx.fcitx5.android.utils.Logcat
import java.io.OutputStreamWriter
import java.util.*

class LogActivity : AppCompatActivity() {

    private lateinit var launcher: ActivityResultLauncher<String>

    private fun registerLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(it)
                        ?.let { OutputStreamWriter(it) }
                }.getOrNull()?.use {
                    Logcat.default
                        .getLogAsync()
                        .await()
                        .onSuccess { lines ->
                            lines.forEach(it::write)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LogActivity, R.string.done, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LogActivity, it.message, Toast.LENGTH_SHORT)
                                    .show()
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
            clearButton.setOnClickListener {
                logView.clear()
            }
            exportButton.setOnClickListener {
                launcher.launch("${DateFormat.format("yyyy-MM-dd-HH:mm:ss", Date())}.txt")
            }
        }
        supportActionBar!!.setTitle(R.string.real_time_logs)
        registerLauncher()
    }

}