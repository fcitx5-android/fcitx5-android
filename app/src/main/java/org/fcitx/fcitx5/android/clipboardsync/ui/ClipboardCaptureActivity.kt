package org.fcitx.fcitx5.android.clipboardsync.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import org.fcitx.fcitx5.android.clipboardsync.MainService
import org.fcitx.fcitx5.android.R

class ClipboardCaptureActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val capturedContent = runCatching {
            val clip = clipboardManager.primaryClip ?: return@runCatching null
            if (clip.itemCount == 0) return@runCatching null
            val item = clip.getItemAt(0)
            item.uri?.toString()
                ?: item.text?.toString()?.takeIf { it.isNotEmpty() }
                ?: item.coerceToText(this)?.toString()?.takeIf { it.isNotEmpty() }
        }.getOrNull()

        val messageRes = if (capturedContent.isNullOrBlank()) {
            R.string.manual_capture_empty
        } else {
            MainService.submitCapturedClipboard(this, capturedContent, "foreground-capture")
            R.string.manual_capture_success
        }
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        finish()
    }
}
