package org.fcitx.fcitx5.android.core

import android.view.KeyEvent

class LinuxKeyCode {
    companion object {
        const val KEY_Y = 21
        const val KEY_U = 22
        const val KEY_F = 33
        const val KEY_L = 38
        const val KEY_SEMICOLON = 39
    }
}

class KeyCode {
    companion object {
        val androidToLinux: HashMap<Int, Int> = hashMapOf(
            KeyEvent.KEYCODE_Y to LinuxKeyCode.KEY_Y,
            KeyEvent.KEYCODE_U to LinuxKeyCode.KEY_U,
            KeyEvent.KEYCODE_F to LinuxKeyCode.KEY_F,
            KeyEvent.KEYCODE_L to LinuxKeyCode.KEY_L,
            KeyEvent.KEYCODE_SEMICOLON to LinuxKeyCode.KEY_SEMICOLON,
        )
    }
}
