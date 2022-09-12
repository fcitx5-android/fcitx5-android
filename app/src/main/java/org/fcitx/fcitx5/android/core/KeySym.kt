package org.fcitx.fcitx5.android.core

import android.view.KeyEvent

data class KeySym(val sym: UInt) {

    val keyCode get() = KeyCode[sym.toInt()] ?: KeyEvent.KEYCODE_UNKNOWN

    fun toInt() = sym.toInt()

    override fun toString() = "0x" + sym.toString(16).padStart(4, '0')

    companion object {
        fun of(v: Int) = KeySym(v.toUInt())

        fun fromKeyEvent(event: KeyEvent) = Sym[event.keyCode]?.let { KeySym(it.toUInt()) }

        val KeyCode: HashMap<Int, Int> = hashMapOf(
            0x0009 to KeyEvent.KEYCODE_TAB, /* U+0009 CHARACTER TABULATION */
            0x0020 to KeyEvent.KEYCODE_SPACE, /* U+0020 SPACE */
//            0x0021 to KeyEvent.KEYCODE_EXCLAM, /* U+0021 EXCLAMATION MARK */
//            0x0022 to KeyEvent.KEYCODE_QUOTEDBL, /* U+0022 QUOTATION MARK */
            0x0023 to KeyEvent.KEYCODE_POUND, /* U+0023 NUMBER SIGN */
//            0x0024 to KeyEvent.KEYCODE_DOLLAR, /* U+0024 DOLLAR SIGN */
//            0x0025 to KeyEvent.KEYCODE_PERCENT, /* U+0025 PERCENT SIGN */
//            0x0026 to KeyEvent.KEYCODE_AMPERSAND, /* U+0026 AMPERSAND */
            0x0027 to KeyEvent.KEYCODE_APOSTROPHE, /* U+0027 APOSTROPHE */
//            0x0027 to KeyEvent.KEYCODE_QUOTERIGHT, /* deprecated */
//            0x0028 to KeyEvent.KEYCODE_PARENLEFT, /* U+0028 LEFT PARENTHESIS */
//            0x0029 to KeyEvent.KEYCODE_PARENRIGHT, /* U+0029 RIGHT PARENTHESIS */
            0x002a to KeyEvent.KEYCODE_STAR, /* U+002A ASTERISK */
            0x002b to KeyEvent.KEYCODE_PLUS, /* U+002B PLUS SIGN */
            0x002c to KeyEvent.KEYCODE_COMMA, /* U+002C COMMA */
            0x002d to KeyEvent.KEYCODE_MINUS, /* U+002D HYPHEN-MINUS */
            0x002e to KeyEvent.KEYCODE_PERIOD, /* U+002E FULL STOP */
            0x002f to KeyEvent.KEYCODE_SLASH, /* U+002F SOLIDUS */
            0x0030 to KeyEvent.KEYCODE_0, /* U+0030 DIGIT ZERO */
            0x0031 to KeyEvent.KEYCODE_1, /* U+0031 DIGIT ONE */
            0x0032 to KeyEvent.KEYCODE_2, /* U+0032 DIGIT TWO */
            0x0033 to KeyEvent.KEYCODE_3, /* U+0033 DIGIT THREE */
            0x0034 to KeyEvent.KEYCODE_4, /* U+0034 DIGIT FOUR */
            0x0035 to KeyEvent.KEYCODE_5, /* U+0035 DIGIT FIVE */
            0x0036 to KeyEvent.KEYCODE_6, /* U+0036 DIGIT SIX */
            0x0037 to KeyEvent.KEYCODE_7, /* U+0037 DIGIT SEVEN */
            0x0038 to KeyEvent.KEYCODE_8, /* U+0038 DIGIT EIGHT */
            0x0039 to KeyEvent.KEYCODE_9, /* U+0039 DIGIT NINE */
//            0x003a to KeyEvent.KEYCODE_COLON, /* U+003A COLON */
            0x003b to KeyEvent.KEYCODE_SEMICOLON, /* U+003B SEMICOLON */
//            0x003c to KeyEvent.KEYCODE_LESS, /* U+003C LESS-THAN SIGN */
            0x003d to KeyEvent.KEYCODE_EQUALS, /* U+003D EQUALS SIGN */
//            0x003e to KeyEvent.KEYCODE_GREATER, /* U+003E GREATER-THAN SIGN */
//            0x003f to KeyEvent.KEYCODE_QUESTION, /* U+003F QUESTION MARK */
            0x0040 to KeyEvent.KEYCODE_AT, /* U+0040 COMMERCIAL AT */
            0x0041 to KeyEvent.KEYCODE_A, /* U+0041 LATIN CAPITAL LETTER A */
            0x0042 to KeyEvent.KEYCODE_B, /* U+0042 LATIN CAPITAL LETTER B */
            0x0043 to KeyEvent.KEYCODE_C, /* U+0043 LATIN CAPITAL LETTER C */
            0x0044 to KeyEvent.KEYCODE_D, /* U+0044 LATIN CAPITAL LETTER D */
            0x0045 to KeyEvent.KEYCODE_E, /* U+0045 LATIN CAPITAL LETTER E */
            0x0046 to KeyEvent.KEYCODE_F, /* U+0046 LATIN CAPITAL LETTER F */
            0x0047 to KeyEvent.KEYCODE_G, /* U+0047 LATIN CAPITAL LETTER G */
            0x0048 to KeyEvent.KEYCODE_H, /* U+0048 LATIN CAPITAL LETTER H */
            0x0049 to KeyEvent.KEYCODE_I, /* U+0049 LATIN CAPITAL LETTER I */
            0x004a to KeyEvent.KEYCODE_J, /* U+004A LATIN CAPITAL LETTER J */
            0x004b to KeyEvent.KEYCODE_K, /* U+004B LATIN CAPITAL LETTER K */
            0x004c to KeyEvent.KEYCODE_L, /* U+004C LATIN CAPITAL LETTER L */
            0x004d to KeyEvent.KEYCODE_M, /* U+004D LATIN CAPITAL LETTER M */
            0x004e to KeyEvent.KEYCODE_N, /* U+004E LATIN CAPITAL LETTER N */
            0x004f to KeyEvent.KEYCODE_O, /* U+004F LATIN CAPITAL LETTER O */
            0x0050 to KeyEvent.KEYCODE_P, /* U+0050 LATIN CAPITAL LETTER P */
            0x0051 to KeyEvent.KEYCODE_Q, /* U+0051 LATIN CAPITAL LETTER Q */
            0x0052 to KeyEvent.KEYCODE_R, /* U+0052 LATIN CAPITAL LETTER R */
            0x0053 to KeyEvent.KEYCODE_S, /* U+0053 LATIN CAPITAL LETTER S */
            0x0054 to KeyEvent.KEYCODE_T, /* U+0054 LATIN CAPITAL LETTER T */
            0x0055 to KeyEvent.KEYCODE_U, /* U+0055 LATIN CAPITAL LETTER U */
            0x0056 to KeyEvent.KEYCODE_V, /* U+0056 LATIN CAPITAL LETTER V */
            0x0057 to KeyEvent.KEYCODE_W, /* U+0057 LATIN CAPITAL LETTER W */
            0x0058 to KeyEvent.KEYCODE_X, /* U+0058 LATIN CAPITAL LETTER X */
            0x0059 to KeyEvent.KEYCODE_Y, /* U+0059 LATIN CAPITAL LETTER Y */
            0x005a to KeyEvent.KEYCODE_Z, /* U+005A LATIN CAPITAL LETTER Z */
            0x005b to KeyEvent.KEYCODE_LEFT_BRACKET, /* U+005B LEFT SQUARE BRACKET */
            0x005c to KeyEvent.KEYCODE_BACKSLASH, /* U+005C REVERSE SOLIDUS */
            0x005d to KeyEvent.KEYCODE_RIGHT_BRACKET, /* U+005D RIGHT SQUARE BRACKET */
//            0x005e to KeyEvent.KEYCODE_ASCIICIRCUM, /* U+005E CIRCUMFLEX ACCENT */
//            0x005f to KeyEvent.KEYCODE_UNDERSCORE, /* U+005F LOW LINE */
            0x0060 to KeyEvent.KEYCODE_GRAVE, /* U+0060 GRAVE ACCENT */
//            0x0060 to KeyEvent.KEYCODE_QUOTELEFT, /* deprecated */
            0x0061 to KeyEvent.KEYCODE_A, /* U+0061 LATIN SMALL LETTER A */
            0x0062 to KeyEvent.KEYCODE_B, /* U+0062 LATIN SMALL LETTER B */
            0x0063 to KeyEvent.KEYCODE_C, /* U+0063 LATIN SMALL LETTER C */
            0x0064 to KeyEvent.KEYCODE_D, /* U+0064 LATIN SMALL LETTER D */
            0x0065 to KeyEvent.KEYCODE_E, /* U+0065 LATIN SMALL LETTER E */
            0x0066 to KeyEvent.KEYCODE_F, /* U+0066 LATIN SMALL LETTER F */
            0x0067 to KeyEvent.KEYCODE_G, /* U+0067 LATIN SMALL LETTER G */
            0x0068 to KeyEvent.KEYCODE_H, /* U+0068 LATIN SMALL LETTER H */
            0x0069 to KeyEvent.KEYCODE_I, /* U+0069 LATIN SMALL LETTER I */
            0x006a to KeyEvent.KEYCODE_J, /* U+006A LATIN SMALL LETTER J */
            0x006b to KeyEvent.KEYCODE_K, /* U+006B LATIN SMALL LETTER K */
            0x006c to KeyEvent.KEYCODE_L, /* U+006C LATIN SMALL LETTER L */
            0x006d to KeyEvent.KEYCODE_M, /* U+006D LATIN SMALL LETTER M */
            0x006e to KeyEvent.KEYCODE_N, /* U+006E LATIN SMALL LETTER N */
            0x006f to KeyEvent.KEYCODE_O, /* U+006F LATIN SMALL LETTER O */
            0x0070 to KeyEvent.KEYCODE_P, /* U+0070 LATIN SMALL LETTER P */
            0x0071 to KeyEvent.KEYCODE_Q, /* U+0071 LATIN SMALL LETTER Q */
            0x0072 to KeyEvent.KEYCODE_R, /* U+0072 LATIN SMALL LETTER R */
            0x0073 to KeyEvent.KEYCODE_S, /* U+0073 LATIN SMALL LETTER S */
            0x0074 to KeyEvent.KEYCODE_T, /* U+0074 LATIN SMALL LETTER T */
            0x0075 to KeyEvent.KEYCODE_U, /* U+0075 LATIN SMALL LETTER U */
            0x0076 to KeyEvent.KEYCODE_V, /* U+0076 LATIN SMALL LETTER V */
            0x0077 to KeyEvent.KEYCODE_W, /* U+0077 LATIN SMALL LETTER W */
            0x0078 to KeyEvent.KEYCODE_X, /* U+0078 LATIN SMALL LETTER X */
            0x0079 to KeyEvent.KEYCODE_Y, /* U+0079 LATIN SMALL LETTER Y */
            0x007a to KeyEvent.KEYCODE_Z, /* U+007A LATIN SMALL LETTER Z */
//            0x007b to KeyEvent.KEYCODE_BRACELEFT, /* U+007B LEFT CURLY BRACKET */
//            0x007c to KeyEvent.KEYCODE_BAR, /* U+007C VERTICAL LINE */
//            0x007d to KeyEvent.KEYCODE_BRACERIGHT, /* U+007D RIGHT CURLY BRACKET */
//            0x007e to KeyEvent.KEYCODE_ASCIITILDE, /* U+007E TILDE */

            0xffbe to KeyEvent.KEYCODE_F1,
            0xffbf to KeyEvent.KEYCODE_F2,
            0xffc0 to KeyEvent.KEYCODE_F3,
            0xffc1 to KeyEvent.KEYCODE_F4,
            0xffc2 to KeyEvent.KEYCODE_F5,
            0xffc3 to KeyEvent.KEYCODE_F6,
            0xffc4 to KeyEvent.KEYCODE_F7,
            0xffc5 to KeyEvent.KEYCODE_F8,
            0xffc6 to KeyEvent.KEYCODE_F9,
            0xffc7 to KeyEvent.KEYCODE_F10,
            0xffc8 to KeyEvent.KEYCODE_F11,
            0xffc9 to KeyEvent.KEYCODE_F12,

            0xffe1 to KeyEvent.KEYCODE_SHIFT_LEFT,
            0xffe2 to KeyEvent.KEYCODE_SHIFT_RIGHT,
            0xffe3 to KeyEvent.KEYCODE_CTRL_LEFT,
            0xffe4 to KeyEvent.KEYCODE_CTRL_RIGHT,
            0xffe5 to KeyEvent.KEYCODE_CAPS_LOCK,
            0xffe7 to KeyEvent.KEYCODE_META_LEFT,
            0xffe8 to KeyEvent.KEYCODE_META_RIGHT,
            0xffe9 to KeyEvent.KEYCODE_ALT_LEFT,
            0xffea to KeyEvent.KEYCODE_ALT_RIGHT,

            0xff63 to KeyEvent.KEYCODE_INSERT,
            0xffff to KeyEvent.KEYCODE_FORWARD_DEL, // Delete
            0xff50 to KeyEvent.KEYCODE_MOVE_HOME,
            0xff57 to KeyEvent.KEYCODE_MOVE_END,
            0xff56 to KeyEvent.KEYCODE_PAGE_DOWN,
            0xff55 to KeyEvent.KEYCODE_PAGE_UP,
            0xff09 to KeyEvent.KEYCODE_TAB,
            0xff08 to KeyEvent.KEYCODE_DEL, // BackSpace
            0xff0d to KeyEvent.KEYCODE_ENTER,
            0xff1b to KeyEvent.KEYCODE_ESCAPE,

            0xff52 to KeyEvent.KEYCODE_DPAD_UP,
            0xff54 to KeyEvent.KEYCODE_DPAD_DOWN,
            0xff51 to KeyEvent.KEYCODE_DPAD_LEFT,
            0xff53 to KeyEvent.KEYCODE_DPAD_RIGHT,

            0xffaf to KeyEvent.KEYCODE_NUMPAD_DIVIDE,
            0xffaa to KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
            0xffad to KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
            0xffb7 to KeyEvent.KEYCODE_NUMPAD_7,
            0xffb8 to KeyEvent.KEYCODE_NUMPAD_8,
            0xffb9 to KeyEvent.KEYCODE_NUMPAD_9,
            0xffab to KeyEvent.KEYCODE_NUMPAD_ADD,
            0xffb4 to KeyEvent.KEYCODE_NUMPAD_4,
            0xffb5 to KeyEvent.KEYCODE_NUMPAD_5,
            0xffb6 to KeyEvent.KEYCODE_NUMPAD_6,
            0xffb1 to KeyEvent.KEYCODE_NUMPAD_1,
            0xffb2 to KeyEvent.KEYCODE_NUMPAD_2,
            0xffb3 to KeyEvent.KEYCODE_NUMPAD_3,
            0xff8d to KeyEvent.KEYCODE_NUMPAD_ENTER,
            0xffb0 to KeyEvent.KEYCODE_NUMPAD_0,
            0xffae to KeyEvent.KEYCODE_NUMPAD_DOT,

            0xff30 to KeyEvent.KEYCODE_EISU, // FcitxKey_Eisu_toggle
            0xff2d to KeyEvent.KEYCODE_KANA, // FcitxKey_Kana_Lock
            0xff27 to KeyEvent.KEYCODE_KATAKANA_HIRAGANA, // FcitxKey_Hiragana_Katakana
            0xff2a to KeyEvent.KEYCODE_ZENKAKU_HANKAKU, // FcitxKey_Zenkaku_Hankaku
        )

        private val Sym = HashMap<Int, Int>().also {
            KeyCode.forEach { (k, v) ->
                // exclude uppercase latin letter range because:
                // - there is not separate KeyCode for upper and lower case characters
                // - ASCII printable characters have same KeySym value as their char code
                // - they should produce different KeySym when hold Shift
                // TODO: map (keyCode with metaState) to (KeySym with KeyStates) at once
                if (0x0041 > k || k > 0x005a) it[v] = k
            }
        }
    }
}