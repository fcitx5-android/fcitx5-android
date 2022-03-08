package me.rocka.fcitx5test.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import me.rocka.fcitx5test.input.FcitxInputMethodService

object InputMethodUtil {
    private val serviceName =
        ComponentName(appContext, FcitxInputMethodService::class.java).flattenToShortString()

    fun isEnabled() = serviceName in Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS
    ).split(':')

    fun isSelected() = serviceName == Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )

    fun startSettingsActivity(context: Context) =
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))

    fun showSelector(context: Context) =
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showInputMethodPicker()
}