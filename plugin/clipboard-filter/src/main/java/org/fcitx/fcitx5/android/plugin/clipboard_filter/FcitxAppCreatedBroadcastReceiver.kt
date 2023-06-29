package org.fcitx.fcitx5.android.plugin.clipboard_filter

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class FcitxAppCreatedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MyBroadcastReceiver", "Received fcitx application created")
        context
            .getSystemService(JobScheduler::class.java)
            .schedule(
                JobInfo.Builder(233, ComponentName(context, ClearURLsService::class.java))
                    .setMinimumLatency(1)
                    .setOverrideDeadline(10000)
                    .build()
            )
    }
}
