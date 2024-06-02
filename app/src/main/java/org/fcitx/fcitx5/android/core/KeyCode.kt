package org.fcitx.fcitx5.android.core

import android.view.KeyEvent

class LinuxKeyCode {
    companion object {
        const val KEY_ESC = 1
        const val KEY_1 = 2
        const val KEY_2 = 3
        const val KEY_3 = 4
        const val KEY_4 = 5
        const val KEY_5 = 6
        const val KEY_6 = 7
        const val KEY_7 = 8
        const val KEY_8 = 9
        const val KEY_9 = 10
        const val KEY_0 = 11
        const val KEY_MINUS = 12
        const val KEY_EQUAL = 13
        const val KEY_BACKSPACE = 14
        const val KEY_TAB = 15
        const val KEY_Q = 16
        const val KEY_W = 17
        const val KEY_E = 18
        const val KEY_R = 19
        const val KEY_T = 20
        const val KEY_Y = 21
        const val KEY_U = 22
        const val KEY_I = 23
        const val KEY_O = 24
        const val KEY_P = 25
        const val KEY_LEFTBRACE = 26
        const val KEY_RIGHTBRACE = 27
        const val KEY_ENTER = 28
        const val KEY_LEFTCTRL = 29
        const val KEY_A = 30
        const val KEY_S = 31
        const val KEY_D = 32
        const val KEY_F = 33
        const val KEY_G = 34
        const val KEY_H = 35
        const val KEY_J = 36
        const val KEY_K = 37
        const val KEY_L = 38
        const val KEY_SEMICOLON = 39
        const val KEY_APOSTROPHE = 40
        const val KEY_GRAVE = 41
        const val KEY_LEFTSHIFT = 42
        const val KEY_BACKSLASH = 43
        const val KEY_Z = 44
        const val KEY_X = 45
        const val KEY_C = 46
        const val KEY_V = 47
        const val KEY_B = 48
        const val KEY_N = 49
        const val KEY_M = 50
        const val KEY_COMMA = 51
        const val KEY_DOT = 52
        const val KEY_SLASH = 53
        const val KEY_RIGHTSHIFT = 54
        const val KEY_KPASTERISK = 55
        const val KEY_LEFTALT = 56
        const val KEY_SPACE = 57
        const val KEY_CAPSLOCK = 58
        const val KEY_F1 = 59
        const val KEY_F2 = 60
        const val KEY_F3 = 61
        const val KEY_F4 = 62
        const val KEY_F5 = 63
        const val KEY_F6 = 64
        const val KEY_F7 = 65
        const val KEY_F8 = 66
        const val KEY_F9 = 67
        const val KEY_F10 = 68
        const val KEY_NUMLOCK = 69
        const val KEY_SCROLLLOCK = 70
        const val KEY_KP7 = 71
        const val KEY_KP8 = 72
        const val KEY_KP9 = 73
        const val KEY_KPMINUS = 74
        const val KEY_KP4 = 75
        const val KEY_KP5 = 76
        const val KEY_KP6 = 77
        const val KEY_KPPLUS = 78
        const val KEY_KP1 = 79
        const val KEY_KP2 = 80
        const val KEY_KP3 = 81
        const val KEY_KP0 = 82
        const val KEY_KPDOT = 83
        const val KEY_F11 = 87
        const val KEY_F12 = 88
        const val KEY_KPENTER = 96
        const val KEY_RIGHTCTRL = 97
        const val KEY_KPSLASH = 98
        const val KEY_SYSRQ = 99
        const val KEY_HOME = 102
        const val KEY_PAGEUP = 104
        const val KEY_END = 107
        const val KEY_PAGEDOWN = 109
        const val KEY_INSERT = 110
        const val KEY_DELETE = 111
        const val KEY_KPEQUAL = 117
        const val KEY_PAUSE = 119
        const val KEY_KPCOMMA = 121
        const val KEY_LEFTMETA = 125
        const val KEY_RIGHTMETA = 126
        const val KEY_KPLEFTPAREN = 179
        const val KEY_KPRIGHTPAREN = 180
    }
}

class KeyCode {
    companion object {
        val androidToLinux: HashMap<Int, Int> = hashMapOf(
            KeyEvent.KEYCODE_0 to LinuxKeyCode.KEY_0,
            KeyEvent.KEYCODE_1 to LinuxKeyCode.KEY_1,
            KeyEvent.KEYCODE_2 to LinuxKeyCode.KEY_2,
            KeyEvent.KEYCODE_3 to LinuxKeyCode.KEY_3,
            KeyEvent.KEYCODE_4 to LinuxKeyCode.KEY_4,
            KeyEvent.KEYCODE_5 to LinuxKeyCode.KEY_5,
            KeyEvent.KEYCODE_6 to LinuxKeyCode.KEY_6,
            KeyEvent.KEYCODE_7 to LinuxKeyCode.KEY_7,
            KeyEvent.KEYCODE_8 to LinuxKeyCode.KEY_8,
            KeyEvent.KEYCODE_9 to LinuxKeyCode.KEY_9,

            KeyEvent.KEYCODE_A to LinuxKeyCode.KEY_A,
            KeyEvent.KEYCODE_B to LinuxKeyCode.KEY_B,
            KeyEvent.KEYCODE_C to LinuxKeyCode.KEY_C,
            KeyEvent.KEYCODE_D to LinuxKeyCode.KEY_D,
            KeyEvent.KEYCODE_E to LinuxKeyCode.KEY_E,
            KeyEvent.KEYCODE_F to LinuxKeyCode.KEY_F,
            KeyEvent.KEYCODE_G to LinuxKeyCode.KEY_G,
            KeyEvent.KEYCODE_H to LinuxKeyCode.KEY_H,
            KeyEvent.KEYCODE_I to LinuxKeyCode.KEY_I,
            KeyEvent.KEYCODE_J to LinuxKeyCode.KEY_J,
            KeyEvent.KEYCODE_K to LinuxKeyCode.KEY_K,
            KeyEvent.KEYCODE_L to LinuxKeyCode.KEY_L,
            KeyEvent.KEYCODE_M to LinuxKeyCode.KEY_M,
            KeyEvent.KEYCODE_N to LinuxKeyCode.KEY_N,
            KeyEvent.KEYCODE_O to LinuxKeyCode.KEY_O,
            KeyEvent.KEYCODE_P to LinuxKeyCode.KEY_P,
            KeyEvent.KEYCODE_Q to LinuxKeyCode.KEY_Q,
            KeyEvent.KEYCODE_R to LinuxKeyCode.KEY_R,
            KeyEvent.KEYCODE_S to LinuxKeyCode.KEY_S,
            KeyEvent.KEYCODE_T to LinuxKeyCode.KEY_T,
            KeyEvent.KEYCODE_U to LinuxKeyCode.KEY_U,
            KeyEvent.KEYCODE_V to LinuxKeyCode.KEY_V,
            KeyEvent.KEYCODE_W to LinuxKeyCode.KEY_W,
            KeyEvent.KEYCODE_X to LinuxKeyCode.KEY_X,
            KeyEvent.KEYCODE_Y to LinuxKeyCode.KEY_Y,
            KeyEvent.KEYCODE_Z to LinuxKeyCode.KEY_Z,

            KeyEvent.KEYCODE_COMMA to LinuxKeyCode.KEY_COMMA,
            KeyEvent.KEYCODE_PERIOD to LinuxKeyCode.KEY_DOT,
            KeyEvent.KEYCODE_ALT_LEFT to LinuxKeyCode.KEY_LEFTALT,
            KeyEvent.KEYCODE_ALT_RIGHT to LinuxKeyCode.KEY_LEFTALT,
            KeyEvent.KEYCODE_SHIFT_LEFT to LinuxKeyCode.KEY_LEFTSHIFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT to LinuxKeyCode.KEY_RIGHTSHIFT,
            KeyEvent.KEYCODE_TAB to LinuxKeyCode.KEY_TAB,
            KeyEvent.KEYCODE_SPACE to LinuxKeyCode.KEY_SPACE,
            KeyEvent.KEYCODE_ENTER to LinuxKeyCode.KEY_ENTER,
            KeyEvent.KEYCODE_DEL to LinuxKeyCode.KEY_BACKSPACE,
            KeyEvent.KEYCODE_GRAVE to LinuxKeyCode.KEY_GRAVE,
            KeyEvent.KEYCODE_MINUS to LinuxKeyCode.KEY_MINUS,
            KeyEvent.KEYCODE_EQUALS to LinuxKeyCode.KEY_EQUAL,
            KeyEvent.KEYCODE_LEFT_BRACKET to LinuxKeyCode.KEY_LEFTBRACE,
            KeyEvent.KEYCODE_RIGHT_BRACKET to LinuxKeyCode.KEY_RIGHTBRACE,
            KeyEvent.KEYCODE_BACKSLASH to LinuxKeyCode.KEY_BACKSLASH,
            KeyEvent.KEYCODE_SEMICOLON to LinuxKeyCode.KEY_SEMICOLON,
            KeyEvent.KEYCODE_APOSTROPHE to LinuxKeyCode.KEY_APOSTROPHE,
            KeyEvent.KEYCODE_SLASH to LinuxKeyCode.KEY_SLASH,
            KeyEvent.KEYCODE_PAGE_UP to LinuxKeyCode.KEY_PAGEUP,
            KeyEvent.KEYCODE_PAGE_DOWN to LinuxKeyCode.KEY_PAGEDOWN,
            KeyEvent.KEYCODE_ESCAPE to LinuxKeyCode.KEY_ESC,
            KeyEvent.KEYCODE_FORWARD_DEL to LinuxKeyCode.KEY_DELETE,
            KeyEvent.KEYCODE_CTRL_LEFT to LinuxKeyCode.KEY_LEFTCTRL,
            KeyEvent.KEYCODE_CTRL_RIGHT to LinuxKeyCode.KEY_RIGHTCTRL,
            KeyEvent.KEYCODE_CAPS_LOCK to LinuxKeyCode.KEY_CAPSLOCK,
            KeyEvent.KEYCODE_SCROLL_LOCK to LinuxKeyCode.KEY_SCROLLLOCK,
            KeyEvent.KEYCODE_META_LEFT to LinuxKeyCode.KEY_LEFTMETA,
            KeyEvent.KEYCODE_META_RIGHT to LinuxKeyCode.KEY_RIGHTMETA,
            KeyEvent.KEYCODE_SYSRQ to LinuxKeyCode.KEY_SYSRQ,
            KeyEvent.KEYCODE_BREAK to LinuxKeyCode.KEY_PAUSE,
            KeyEvent.KEYCODE_MOVE_HOME to LinuxKeyCode.KEY_HOME,
            KeyEvent.KEYCODE_MOVE_END to LinuxKeyCode.KEY_END,
            KeyEvent.KEYCODE_INSERT to LinuxKeyCode.KEY_INSERT,
            KeyEvent.KEYCODE_F1 to LinuxKeyCode.KEY_F1,
            KeyEvent.KEYCODE_F2 to LinuxKeyCode.KEY_F2,
            KeyEvent.KEYCODE_F3 to LinuxKeyCode.KEY_F3,
            KeyEvent.KEYCODE_F4 to LinuxKeyCode.KEY_F4,
            KeyEvent.KEYCODE_F5 to LinuxKeyCode.KEY_F5,
            KeyEvent.KEYCODE_F6 to LinuxKeyCode.KEY_F6,
            KeyEvent.KEYCODE_F7 to LinuxKeyCode.KEY_F7,
            KeyEvent.KEYCODE_F8 to LinuxKeyCode.KEY_F8,
            KeyEvent.KEYCODE_F9 to LinuxKeyCode.KEY_F9,
            KeyEvent.KEYCODE_F10 to LinuxKeyCode.KEY_F10,
            KeyEvent.KEYCODE_F11 to LinuxKeyCode.KEY_F11,
            KeyEvent.KEYCODE_F12 to LinuxKeyCode.KEY_F12,
            KeyEvent.KEYCODE_NUM_LOCK to LinuxKeyCode.KEY_NUMLOCK,
            KeyEvent.KEYCODE_NUMPAD_0 to LinuxKeyCode.KEY_KP0,
            KeyEvent.KEYCODE_NUMPAD_1 to LinuxKeyCode.KEY_KP1,
            KeyEvent.KEYCODE_NUMPAD_2 to LinuxKeyCode.KEY_KP2,
            KeyEvent.KEYCODE_NUMPAD_3 to LinuxKeyCode.KEY_KP3,
            KeyEvent.KEYCODE_NUMPAD_4 to LinuxKeyCode.KEY_KP4,
            KeyEvent.KEYCODE_NUMPAD_5 to LinuxKeyCode.KEY_KP5,
            KeyEvent.KEYCODE_NUMPAD_6 to LinuxKeyCode.KEY_KP6,
            KeyEvent.KEYCODE_NUMPAD_7 to LinuxKeyCode.KEY_KP7,
            KeyEvent.KEYCODE_NUMPAD_8 to LinuxKeyCode.KEY_KP8,
            KeyEvent.KEYCODE_NUMPAD_9 to LinuxKeyCode.KEY_KP9,
            KeyEvent.KEYCODE_NUMPAD_DIVIDE to LinuxKeyCode.KEY_KPSLASH,
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY to LinuxKeyCode.KEY_KPASTERISK,
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT to LinuxKeyCode.KEY_KPMINUS,
            KeyEvent.KEYCODE_NUMPAD_ADD to LinuxKeyCode.KEY_KPPLUS,
            KeyEvent.KEYCODE_NUMPAD_DOT to LinuxKeyCode.KEY_KPDOT,
            KeyEvent.KEYCODE_NUMPAD_COMMA to LinuxKeyCode.KEY_KPCOMMA,
            KeyEvent.KEYCODE_NUMPAD_ENTER to LinuxKeyCode.KEY_KPENTER,
            KeyEvent.KEYCODE_NUMPAD_EQUALS to LinuxKeyCode.KEY_KPEQUAL,
            KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN to LinuxKeyCode.KEY_KPLEFTPAREN,
            KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN to LinuxKeyCode.KEY_KPRIGHTPAREN,
        )
        // XXX: assume QWERT
        var symToCode: HashMap<String, Int> = hashMapOf(
            "0" to LinuxKeyCode.KEY_0,
            "1" to LinuxKeyCode.KEY_1,
            "2" to LinuxKeyCode.KEY_2,
            "3" to LinuxKeyCode.KEY_3,
            "4" to LinuxKeyCode.KEY_4,
            "5" to LinuxKeyCode.KEY_5,
            "6" to LinuxKeyCode.KEY_6,
            "7" to LinuxKeyCode.KEY_7,
            "8" to LinuxKeyCode.KEY_8,
            "9" to LinuxKeyCode.KEY_9,
            "-" to LinuxKeyCode.KEY_MINUS,
            "=" to LinuxKeyCode.KEY_EQUAL,
            "[" to LinuxKeyCode.KEY_LEFTBRACE,
            "]" to LinuxKeyCode.KEY_RIGHTBRACE,
            "\\" to LinuxKeyCode.KEY_BACKSLASH,
            ";" to LinuxKeyCode.KEY_SEMICOLON,
            "'" to LinuxKeyCode.KEY_APOSTROPHE,
            "," to LinuxKeyCode.KEY_COMMA,
            "." to LinuxKeyCode.KEY_DOT,
            "/" to LinuxKeyCode.KEY_SLASH,
        )
    }
}
