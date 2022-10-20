package org.fcitx.fcitx5.android.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.main.ClipboardEditActivity
import org.fcitx.fcitx5.android.ui.main.LogActivity
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodConfigFragment

object AppUtil {

    fun launchMain(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    private fun launchMainToDest(context: Context, @IdRes dest: Int, arguments: Bundle? = null) {
        NavDeepLinkBuilder(context)
            .setGraph(R.navigation.settings_nav)
            .addDestination(dest, arguments)
            .createPendingIntent()
            .send()
    }

    fun launchMainToGlobalOptions(context: Context) =
        launchMainToDest(context, R.id.globalConfigFragment)

    fun launchMainToKeyboard(context: Context) =
        launchMainToDest(context, R.id.keyboardSettingsFragment)

    fun launchMainToInputMethodList(context: Context) =
        launchMainToDest(context, R.id.imListFragment)

    fun launchMainToThemeList(context: Context) =
        launchMainToDest(context, R.id.themeListFragment)

    fun launchMainToInputMethodConfig(context: Context, uniqueName: String, displayName: String) =
        launchMainToDest(
            context, R.id.imConfigFragment, bundleOf(
                InputMethodConfigFragment.ARG_NAME to displayName,
                InputMethodConfigFragment.ARG_UNIQUE_NAME to uniqueName
            )
        )

    fun launchClipboardEdit(context: Context, id: Int) {
        context.startActivity(
            Intent(context, ClipboardEditActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ClipboardEditActivity.ENTRY_ID, id)
            }
        )
    }

}