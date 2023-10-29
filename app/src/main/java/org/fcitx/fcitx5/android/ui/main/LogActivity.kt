/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import org.fcitx.fcitx5.android.utils.DeviceInfo
import org.fcitx.fcitx5.android.utils.Logcat
import org.fcitx.fcitx5.android.utils.applyTranslucentSystemBars
import org.fcitx.fcitx5.android.utils.iso8601UTCDateTime
import org.fcitx.fcitx5.android.utils.toast
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.views.topPadding

class LogActivity : AppCompatActivity() {

    private var fromCrash = false

    private lateinit var launcher: ActivityResultLauncher<String>
    private lateinit var logView: LogView

    private fun registerLauncher() {
        launcher = registerForActivityResult(CreateDocument("text/plain")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)!!.use { stream ->
                        stream.bufferedWriter().use { writer ->
                            writer.write(DeviceInfo.get(this@LogActivity))
                            writer.write(logView.currentLog)
                        }
                    }
                }.toast(this@LogActivity)
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
            }
            binding.toolbar.topPadding = statusBars.top
            binding.logView.setBottomPadding(navBars.bottom)
            windowInsets
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        with(binding) {
            setSupportActionBar(toolbar)
            this@LogActivity.logView = logView
            if (intent.hasExtra(FROM_CRASH)) {
                fromCrash = true
                supportActionBar!!.setTitle(R.string.crash_logs)
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
        }
        registerLauncher()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!fromCrash) {
            menu.add(R.string.clear).apply {
                icon = drawable(R.drawable.ic_baseline_delete_24)!!.apply {
                    setTint(styledColor(android.R.attr.colorControlNormal))
                }
                setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                setOnMenuItemClickListener {
                    logView.clear()
                    true
                }
            }
        }
        menu.add(R.string.export).apply {
            icon = drawable(R.drawable.ic_baseline_save_24)!!.apply {
                setTint(styledColor(android.R.attr.colorControlNormal))
            }
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setOnMenuItemClickListener {
                launcher.launch("$packageName-${iso8601UTCDateTime()}.txt")
                true
            }
        }
        return true
    }

    companion object {
        const val FROM_CRASH = "from_crash"
        const val CRASH_STACK_TRACE = "crash_stack_trace"
    }
}