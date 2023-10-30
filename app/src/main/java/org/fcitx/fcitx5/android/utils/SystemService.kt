package org.fcitx.fcitx5.android.utils

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.os.UserManager
import android.os.Vibrator
import android.os.storage.StorageManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment

val Context.audioManager
    get() = getSystemService<AudioManager>()!!

val Context.clipboardManager
    get() = getSystemService<ClipboardManager>()!!

val Context.inputMethodManager
    get() = getSystemService<InputMethodManager>()!!

val Context.notificationManager
    get() = getSystemService<NotificationManager>()!!

val Fragment.notificationManager
    get() = requireContext().notificationManager

val Context.storageManager
    get() = getSystemService<StorageManager>()!!

val Context.vibrator
    get() = getSystemService<Vibrator>()!!

val Context.userManager
    get() = getSystemService<UserManager>()!!
