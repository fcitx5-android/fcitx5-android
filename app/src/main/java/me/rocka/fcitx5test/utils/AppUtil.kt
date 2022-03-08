package me.rocka.fcitx5test.utils

import android.content.Context
import android.content.Intent
import me.rocka.fcitx5test.ui.main.MainActivity

object AppUtil {
    fun launchMain(context: Context) {
        context.startActivity(
            Intent(
                context,
                MainActivity::class.java
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun launchMainToAddInputMethods(context: Context) {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.INTENT_DATA_ADD_IM, 0)
        })
    }

}