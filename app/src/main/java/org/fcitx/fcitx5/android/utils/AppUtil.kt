package org.fcitx.fcitx5.android.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.fcitx.fcitx5.android.ui.main.LogActivity
import org.fcitx.fcitx5.android.ui.main.MainActivity

object AppUtil {
    fun launchMain(context: Context, initIntent: Intent.() -> Unit = {}) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                initIntent.invoke(this)
            }
        )
    }

    fun launchLog(context: Context, initIntent: Intent.() -> Unit = {}) {
        context.startActivity(
            Intent(context, LogActivity::class.java).apply {
                initIntent.invoke(this)
            }
        )
    }

    fun launchMainToConfig(context: Context, category: String, arguments: Bundle? = null) {
        launchMain(context) {
            putExtra(MainActivity.INTENT_DATA_CONFIG, category)
            putExtra(category, arguments)
        }
    }

}