/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.codegen

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo


private typealias FcitxKeyName = String
private typealias FcitxKeySym = Int
private typealias AndroidKeyCode = String
private typealias KeyPair = Pair<Pair<FcitxKeyName, FcitxKeySym>, AndroidKeyCode>

internal class GenKeyMappingProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private val pairs: List<KeyPair> = listOf(
        "space" to 0x0020 to "KEYCODE_SPACE", /* U+0020 SPACE */
//            0x0021 to KeyEvent.KEYCODE_EXCLAM, /* U+0021 EXCLAMATION MARK */
//            0x0022 to KeyEvent.KEYCODE_QUOTEDBL, /* U+0022 QUOTATION MARK */
        "numbersign" to 0x0023 to "KEYCODE_POUND", /* U+0023 NUMBER SIGN */
//            0x0024 to KeyEvent.KEYCODE_DOLLAR, /* U+0024 DOLLAR SIGN */
//            0x0025 to KeyEvent.KEYCODE_PERCENT, /* U+0025 PERCENT SIGN */
//            0x0026 to KeyEvent.KEYCODE_AMPERSAND, /* U+0026 AMPERSAND */
        "apostrophe" to 0x0027 to "KEYCODE_APOSTROPHE", /* U+0027 APOSTROPHE */
//            0x0027 to KeyEvent.KEYCODE_QUOTERIGHT, /* deprecated */
//            0x0028 to KeyEvent.KEYCODE_PARENLEFT, /* U+0028 LEFT PARENTHESIS */
//            0x0029 to KeyEvent.KEYCODE_PARENRIGHT, /* U+0029 RIGHT PARENTHESIS */
        "asterisk" to 0x002a to "KEYCODE_STAR", /* U+002A ASTERISK */
        "plus" to 0x002b to "KEYCODE_PLUS", /* U+002B PLUS SIGN */
        "comma" to 0x002c to "KEYCODE_COMMA", /* U+002C COMMA */
        "minus" to 0x002d to "KEYCODE_MINUS", /* U+002D HYPHEN-MINUS */
        "period" to 0x002e to "KEYCODE_PERIOD", /* U+002E FULL STOP */
        "slash" to 0x002f to "KEYCODE_SLASH", /* U+002F SOLIDUS */
        "0" to 0x0030 to "KEYCODE_0", /* U+0030 DIGIT ZERO */
        "1" to 0x0031 to "KEYCODE_1", /* U+0031 DIGIT ONE */
        "2" to 0x0032 to "KEYCODE_2", /* U+0032 DIGIT TWO */
        "3" to 0x0033 to "KEYCODE_3", /* U+0033 DIGIT THREE */
        "4" to 0x0034 to "KEYCODE_4", /* U+0034 DIGIT FOUR */
        "5" to 0x0035 to "KEYCODE_5", /* U+0035 DIGIT FIVE */
        "6" to 0x0036 to "KEYCODE_6", /* U+0036 DIGIT SIX */
        "7" to 0x0037 to "KEYCODE_7", /* U+0037 DIGIT SEVEN */
        "8" to 0x0038 to "KEYCODE_8", /* U+0038 DIGIT EIGHT */
        "9" to 0x0039 to "KEYCODE_9", /* U+0039 DIGIT NINE */
//            0x003a to KeyEvent.KEYCODE_COLON, /* U+003A COLON */
        "semicolon" to 0x003b to "KEYCODE_SEMICOLON", /* U+003B SEMICOLON */
//            0x003c to KeyEvent.KEYCODE_LESS, /* U+003C LESS-THAN SIGN */
        "equal" to 0x003d to "KEYCODE_EQUALS", /* U+003D EQUALS SIGN */
//            0x003e to KeyEvent.KEYCODE_GREATER, /* U+003E GREATER-THAN SIGN */
//            0x003f to KeyEvent.KEYCODE_QUESTION, /* U+003F QUESTION MARK */
        "at" to 0x0040 to "KEYCODE_AT", /* U+0040 COMMERCIAL AT */
        "A" to 0x0041 to "KEYCODE_A", /* U+0041 LATIN CAPITAL LETTER A */
        "B" to 0x0042 to "KEYCODE_B", /* U+0042 LATIN CAPITAL LETTER B */
        "C" to 0x0043 to "KEYCODE_C", /* U+0043 LATIN CAPITAL LETTER C */
        "D" to 0x0044 to "KEYCODE_D", /* U+0044 LATIN CAPITAL LETTER D */
        "E" to 0x0045 to "KEYCODE_E", /* U+0045 LATIN CAPITAL LETTER E */
        "F" to 0x0046 to "KEYCODE_F", /* U+0046 LATIN CAPITAL LETTER F */
        "G" to 0x0047 to "KEYCODE_G", /* U+0047 LATIN CAPITAL LETTER G */
        "H" to 0x0048 to "KEYCODE_H", /* U+0048 LATIN CAPITAL LETTER H */
        "I" to 0x0049 to "KEYCODE_I", /* U+0049 LATIN CAPITAL LETTER I */
        "J" to 0x004a to "KEYCODE_J", /* U+004A LATIN CAPITAL LETTER J */
        "K" to 0x004b to "KEYCODE_K", /* U+004B LATIN CAPITAL LETTER K */
        "L" to 0x004c to "KEYCODE_L", /* U+004C LATIN CAPITAL LETTER L */
        "M" to 0x004d to "KEYCODE_M", /* U+004D LATIN CAPITAL LETTER M */
        "N" to 0x004e to "KEYCODE_N", /* U+004E LATIN CAPITAL LETTER N */
        "O" to 0x004f to "KEYCODE_O", /* U+004F LATIN CAPITAL LETTER O */
        "P" to 0x0050 to "KEYCODE_P", /* U+0050 LATIN CAPITAL LETTER P */
        "Q" to 0x0051 to "KEYCODE_Q", /* U+0051 LATIN CAPITAL LETTER Q */
        "R" to 0x0052 to "KEYCODE_R", /* U+0052 LATIN CAPITAL LETTER R */
        "S" to 0x0053 to "KEYCODE_S", /* U+0053 LATIN CAPITAL LETTER S */
        "T" to 0x0054 to "KEYCODE_T", /* U+0054 LATIN CAPITAL LETTER T */
        "U" to 0x0055 to "KEYCODE_U", /* U+0055 LATIN CAPITAL LETTER U */
        "V" to 0x0056 to "KEYCODE_V", /* U+0056 LATIN CAPITAL LETTER V */
        "W" to 0x0057 to "KEYCODE_W", /* U+0057 LATIN CAPITAL LETTER W */
        "X" to 0x0058 to "KEYCODE_X", /* U+0058 LATIN CAPITAL LETTER X */
        "Y" to 0x0059 to "KEYCODE_Y", /* U+0059 LATIN CAPITAL LETTER Y */
        "Z" to 0x005a to "KEYCODE_Z", /* U+005A LATIN CAPITAL LETTER Z */
        "bracketleft" to 0x005b to "KEYCODE_LEFT_BRACKET", /* U+005B LEFT SQUARE BRACKET */
        "backslash" to 0x005c to "KEYCODE_BACKSLASH", /* U+005C REVERSE SOLIDUS */
        "bracketright" to 0x005d to "KEYCODE_RIGHT_BRACKET", /* U+005D RIGHT SQUARE BRACKET */
//            0x005e to KeyEvent.KEYCODE_ASCIICIRCUM, /* U+005E CIRCUMFLEX ACCENT */
//            0x005f to KeyEvent.KEYCODE_UNDERSCORE, /* U+005F LOW LINE */
        "grave" to 0x0060 to "KEYCODE_GRAVE", /* U+0060 GRAVE ACCENT */
//            0x0060 to KeyEvent.KEYCODE_QUOTELEFT, /* deprecated */
        "a" to 0x0061 to "KEYCODE_A", /* U+0061 LATIN SMALL LETTER A */
        "b" to 0x0062 to "KEYCODE_B", /* U+0062 LATIN SMALL LETTER B */
        "c" to 0x0063 to "KEYCODE_C", /* U+0063 LATIN SMALL LETTER C */
        "d" to 0x0064 to "KEYCODE_D", /* U+0064 LATIN SMALL LETTER D */
        "e" to 0x0065 to "KEYCODE_E", /* U+0065 LATIN SMALL LETTER E */
        "f" to 0x0066 to "KEYCODE_F", /* U+0066 LATIN SMALL LETTER F */
        "g" to 0x0067 to "KEYCODE_G", /* U+0067 LATIN SMALL LETTER G */
        "h" to 0x0068 to "KEYCODE_H", /* U+0068 LATIN SMALL LETTER H */
        "i" to 0x0069 to "KEYCODE_I", /* U+0069 LATIN SMALL LETTER I */
        "j" to 0x006a to "KEYCODE_J", /* U+006A LATIN SMALL LETTER J */
        "k" to 0x006b to "KEYCODE_K", /* U+006B LATIN SMALL LETTER K */
        "l" to 0x006c to "KEYCODE_L", /* U+006C LATIN SMALL LETTER L */
        "m" to 0x006d to "KEYCODE_M", /* U+006D LATIN SMALL LETTER M */
        "n" to 0x006e to "KEYCODE_N", /* U+006E LATIN SMALL LETTER N */
        "o" to 0x006f to "KEYCODE_O", /* U+006F LATIN SMALL LETTER O */
        "p" to 0x0070 to "KEYCODE_P", /* U+0070 LATIN SMALL LETTER P */
        "q" to 0x0071 to "KEYCODE_Q", /* U+0071 LATIN SMALL LETTER Q */
        "r" to 0x0072 to "KEYCODE_R", /* U+0072 LATIN SMALL LETTER R */
        "s" to 0x0073 to "KEYCODE_S", /* U+0073 LATIN SMALL LETTER S */
        "t" to 0x0074 to "KEYCODE_T", /* U+0074 LATIN SMALL LETTER T */
        "u" to 0x0075 to "KEYCODE_U", /* U+0075 LATIN SMALL LETTER U */
        "v" to 0x0076 to "KEYCODE_V", /* U+0076 LATIN SMALL LETTER V */
        "w" to 0x0077 to "KEYCODE_W", /* U+0077 LATIN SMALL LETTER W */
        "x" to 0x0078 to "KEYCODE_X", /* U+0078 LATIN SMALL LETTER X */
        "y" to 0x0079 to "KEYCODE_Y", /* U+0079 LATIN SMALL LETTER Y */
        "z" to 0x007a to "KEYCODE_Z", /* U+007A LATIN SMALL LETTER Z */
//            0x007b to KeyEvent.KEYCODE_BRACELEFT, /* U+007B LEFT CURLY BRACKET */
//            0x007c to KeyEvent.KEYCODE_BAR, /* U+007C VERTICAL LINE */
//            0x007d to KeyEvent.KEYCODE_BRACERIGHT, /* U+007D RIGHT CURLY BRACKET */
//            0x007e to KeyEvent.KEYCODE_ASCIITILDE, /* U+007E TILDE */

        "F1" to 0xffbe to "KEYCODE_F1",
        "F2" to 0xffbf to "KEYCODE_F2",
        "F3" to 0xffc0 to "KEYCODE_F3",
        "F4" to 0xffc1 to "KEYCODE_F4",
        "F5" to 0xffc2 to "KEYCODE_F5",
        "F6" to 0xffc3 to "KEYCODE_F6",
        "F7" to 0xffc4 to "KEYCODE_F7",
        "F8" to 0xffc5 to "KEYCODE_F8",
        "F9" to 0xffc6 to "KEYCODE_F9",
        "F10" to 0xffc7 to "KEYCODE_F10",
        "F11" to 0xffc8 to "KEYCODE_F11",
        "F12" to 0xffc9 to "KEYCODE_F12",

        "Shift_L" to 0xffe1 to "KEYCODE_SHIFT_LEFT",
        "Shift_R" to 0xffe2 to "KEYCODE_SHIFT_RIGHT",
        "Control_L" to 0xffe3 to "KEYCODE_CTRL_LEFT",
        "Control_R" to 0xffe4 to "KEYCODE_CTRL_RIGHT",
        "Caps_Lock" to 0xffe5 to "KEYCODE_CAPS_LOCK",
        "Meta_L" to 0xffe7 to "KEYCODE_META_LEFT",
        "Meta_R" to 0xffe8 to "KEYCODE_META_RIGHT",
        "Alt_L" to 0xffe9 to "KEYCODE_ALT_LEFT",
        "Alt_R" to 0xffea to "KEYCODE_ALT_RIGHT",

        "Insert" to 0xff63 to "KEYCODE_INSERT",
        "Delete" to 0xffff to "KEYCODE_FORWARD_DEL", // Delete
        "Home" to 0xff50 to "KEYCODE_MOVE_HOME",
        "End" to 0xff57 to "KEYCODE_MOVE_END",
        "Page_Down" to 0xff56 to "KEYCODE_PAGE_DOWN",
        "Page_Up" to 0xff55 to "KEYCODE_PAGE_UP",
        "Tab" to 0xff09 to "KEYCODE_TAB",
        "BackSpace" to 0xff08 to "KEYCODE_DEL", // BackSpace
        "Return" to 0xff0d to "KEYCODE_ENTER",
        "Escape" to 0xff1b to "KEYCODE_ESCAPE",

        "Up" to 0xff52 to "KEYCODE_DPAD_UP",
        "Down" to 0xff54 to "KEYCODE_DPAD_DOWN",
        "Left" to 0xff51 to "KEYCODE_DPAD_LEFT",
        "Right" to 0xff53 to "KEYCODE_DPAD_RIGHT",

        "KP_Divide" to 0xffaf to "KEYCODE_NUMPAD_DIVIDE",
        "KP_Multiply" to 0xffaa to "KEYCODE_NUMPAD_MULTIPLY",
        "KP_Subtract" to 0xffad to "KEYCODE_NUMPAD_SUBTRACT",
        "KP_7" to 0xffb7 to "KEYCODE_NUMPAD_7",
        "KP_8" to 0xffb8 to "KEYCODE_NUMPAD_8",
        "KP_9" to 0xffb9 to "KEYCODE_NUMPAD_9",
        "KP_Add" to 0xffab to "KEYCODE_NUMPAD_ADD",
        "KP_4" to 0xffb4 to "KEYCODE_NUMPAD_4",
        "KP_5" to 0xffb5 to "KEYCODE_NUMPAD_5",
        "KP_6" to 0xffb6 to "KEYCODE_NUMPAD_6",
        "KP_1" to 0xffb1 to "KEYCODE_NUMPAD_1",
        "KP_2" to 0xffb2 to "KEYCODE_NUMPAD_2",
        "KP_3" to 0xffb3 to "KEYCODE_NUMPAD_3",
        "KP_Enter" to 0xff8d to "KEYCODE_NUMPAD_ENTER",
        "KP_0" to 0xffb0 to "KEYCODE_NUMPAD_0",
        "KP_Decimal" to 0xffae to "KEYCODE_NUMPAD_DOT",

        "Eisu_toggle" to 0xff30 to "KEYCODE_EISU", // FcitxKey_Eisu_toggle
        "Kana_Lock" to 0xff2d to "KEYCODE_KANA", // FcitxKey_Kana_Lock
        "Hiragana_Katakana" to 0xff27 to "KEYCODE_KATAKANA_HIRAGANA", // FcitxKey_Hiragana_Katakana
        "Zenkaku_Hankaku" to 0xff2a to "KEYCODE_ZENKAKU_HANKAKU", // FcitxKey_Zenkaku_Hankaku
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // We don't process annotations at all
        return emptyList()
    }

    private fun keyName(p: Pair<FcitxKeyName, FcitxKeySym>) = "FcitxKey_${p.first}"

    override fun finish() {
        val keyCodeFromSym = FunSpec
            .builder("symToKeyCode")
            .addParameter("sym", Int::class)
            .returns(Int::class.asTypeName().copy(nullable = true))
            .addCode(
                """
                | return when (sym) {
                |     ${pairs.joinToString(separator = "\n|     ") { (f, code) -> "${keyName(f)} -> KeyEvent.$code" }}
                |     else -> null
                | }
                """.trimMargin()
            )
            .build()

        val keyCodeToSym = FunSpec
            .builder("keyCodeToSym")
            .addKdoc("Duplicate labels are expected, as the mapping is not one-to-one")
            .addParameter("code", Int::class)
            .returns(Int::class.asTypeName().copy(nullable = true))
            .addCode(
                """
                    | return when (code) {
                    |     ${
                    // exclude uppercase latin letter range because:
                    // - there is not separate KeyCode for upper and lower case characters
                    // - ASCII printable characters have same KeySym value as their char code
                    // - they should produce different KeySym when hold Shift
                    // TODO: map (keyCode with metaState) to (KeySym with KeyStates) at once
                    pairs.filter { it.first.second !in 0x41..0x5a }
                        .joinToString(separator = "\n|     ") { (f, code) ->
                            "KeyEvent.$code -> ${keyName(f)}"
                        }
                }
                    |     else -> null
                    | }
                """.trimMargin()
            )
            .build()

        val obj = TypeSpec
            .objectBuilder("FcitxKeyMapping")
            .addFunction(keyCodeFromSym)
            .addFunction(keyCodeToSym)
            .apply {
                pairs.forEach { (f, _) ->
                    val (_, sym) = f
                    PropertySpec
                        .builder(
                            keyName(f),
                            Int::class,
                            KModifier.CONST
                        )
                        .initializer(String.format("0x%04x", sym))
                        .build()
                        .let { addProperty(it) }
                }
            }
            .build()

        val file = FileSpec
            .builder("org.fcitx.fcitx5.android.core", "FcitxKeyMapping")
            .addType(obj)
            .addImport("android.view", "KeyEvent")
            .build()
        file.writeTo(environment.codeGenerator, false)
    }

}

class GenKeyMappingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        GenKeyMappingProcessor(environment)
}
