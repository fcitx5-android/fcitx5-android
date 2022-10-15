package org.fcitx.fcitx5.android.input.dialog

import android.content.Context
import org.fcitx.fcitx5.android.core.FcitxAPI
import splitties.systemservices.inputMethodManager

data class InputMethodData(
    val uniqueName: String,
    val name: String,
    val ime: Boolean
) {
    companion object {
        suspend fun resolve(fcitx: FcitxAPI, context: Context): List<InputMethodData> {
            val enabled = fcitx.enabledIme()
                .map { InputMethodData(it.uniqueName, it.displayName, false) }
                .toMutableList()
            enabled += inputMethodManager.enabledInputMethodList
                .filter { it.packageName != context.packageName }
                .map {
                    val label = it.loadLabel(context.packageManager).toString()
                    InputMethodData(it.id, label, true)
                }
            return enabled.toList()
        }
    }
}
