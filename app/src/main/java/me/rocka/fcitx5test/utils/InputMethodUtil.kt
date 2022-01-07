package me.rocka.fcitx5test.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import me.rocka.fcitx5test.FcitxApplication
import me.rocka.fcitx5test.keyboard.FcitxInputMethodService

object InputMethodUtil {
    private val context: Context
        get() = FcitxApplication.getInstance().applicationContext
    private val serviceName =
        ComponentName(context, FcitxInputMethodService::class.java).flattenToShortString()

    fun isEnabled() = serviceName in Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS
    ).split(':')

    fun isSelected() = serviceName == Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )

    fun startSettingsActivity(context: Context) =
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))

    fun showSelector(context: Context) =
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showInputMethodPicker()
}