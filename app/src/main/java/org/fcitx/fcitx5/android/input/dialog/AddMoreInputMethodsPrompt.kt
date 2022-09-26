package org.fcitx.fcitx5.android.input.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.utils.AppUtil

object AddMoreInputMethodsPrompt {
    fun build(service: FcitxInputMethodService, context: Context): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.no_more_input_methods)
            .setMessage(R.string.add_more_input_methods)
            .setPositiveButton(R.string.add) { _, _ ->
                AppUtil.launchMainToConfig(service, MainActivity.INTENT_DATA_CONFIG_IM_LIST)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}
