/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.codegen

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

internal class GenScancodeMappingProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    companion object {
        // https://github.com/torvalds/linux/blob/v6.10/include/uapi/linux/input-event-codes.h#L75
        private val SCANCODE_MAP: List<Pair<String, Int>> = listOf(
            "KEY_RESERVED" to 0,
            "KEY_ESC" to 1,
            "KEY_1" to 2,
            "KEY_2" to 3,
            "KEY_3" to 4,
            "KEY_4" to 5,
            "KEY_5" to 6,
            "KEY_6" to 7,
            "KEY_7" to 8,
            "KEY_8" to 9,
            "KEY_9" to 10,
            "KEY_0" to 11,
            "KEY_MINUS" to 12,
            "KEY_EQUAL" to 13,
            "KEY_BACKSPACE" to 14,
            "KEY_TAB" to 15,
            "KEY_Q" to 16,
            "KEY_W" to 17,
            "KEY_E" to 18,
            "KEY_R" to 19,
            "KEY_T" to 20,
            "KEY_Y" to 21,
            "KEY_U" to 22,
            "KEY_I" to 23,
            "KEY_O" to 24,
            "KEY_P" to 25,
            "KEY_LEFTBRACE" to 26,
            "KEY_RIGHTBRACE" to 27,
            "KEY_ENTER" to 28,
            "KEY_LEFTCTRL" to 29,
            "KEY_A" to 30,
            "KEY_S" to 31,
            "KEY_D" to 32,
            "KEY_F" to 33,
            "KEY_G" to 34,
            "KEY_H" to 35,
            "KEY_J" to 36,
            "KEY_K" to 37,
            "KEY_L" to 38,
            "KEY_SEMICOLON" to 39,
            "KEY_APOSTROPHE" to 40,
            "KEY_GRAVE" to 41,
            "KEY_LEFTSHIFT" to 42,
            "KEY_BACKSLASH" to 43,
            "KEY_Z" to 44,
            "KEY_X" to 45,
            "KEY_C" to 46,
            "KEY_V" to 47,
            "KEY_B" to 48,
            "KEY_N" to 49,
            "KEY_M" to 50,
            "KEY_COMMA" to 51,
            "KEY_DOT" to 52,
            "KEY_SLASH" to 53,
            "KEY_RIGHTSHIFT" to 54,
            "KEY_KPASTERISK" to 55,
            "KEY_LEFTALT" to 56,
            "KEY_SPACE" to 57,
            "KEY_CAPSLOCK" to 58,
            "KEY_F1" to 59,
            "KEY_F2" to 60,
            "KEY_F3" to 61,
            "KEY_F4" to 62,
            "KEY_F5" to 63,
            "KEY_F6" to 64,
            "KEY_F7" to 65,
            "KEY_F8" to 66,
            "KEY_F9" to 67,
            "KEY_F10" to 68,
            "KEY_NUMLOCK" to 69,
            "KEY_SCROLLLOCK" to 70,
            "KEY_KP7" to 71,
            "KEY_KP8" to 72,
            "KEY_KP9" to 73,
            "KEY_KPMINUS" to 74,
            "KEY_KP4" to 75,
            "KEY_KP5" to 76,
            "KEY_KP6" to 77,
            "KEY_KPPLUS" to 78,
            "KEY_KP1" to 79,
            "KEY_KP2" to 80,
            "KEY_KP3" to 81,
            "KEY_KP0" to 82,
            "KEY_KPDOT" to 83,

            "KEY_ZENKAKUHANKAKU" to 85,
            "KEY_102ND" to 86,
            "KEY_F11" to 87,
            "KEY_F12" to 88,
            "KEY_RO" to 89,
            "KEY_KATAKANA" to 90,
            "KEY_HIRAGANA" to 91,
            "KEY_HENKAN" to 92,
            "KEY_KATAKANAHIRAGANA" to 93,
            "KEY_MUHENKAN" to 94,
            "KEY_KPJPCOMMA" to 95,
            "KEY_KPENTER" to 96,
            "KEY_RIGHTCTRL" to 97,
            "KEY_KPSLASH" to 98,
            "KEY_SYSRQ" to 99,
            "KEY_RIGHTALT" to 100,
            "KEY_LINEFEED" to 101,
            "KEY_HOME" to 102,
            "KEY_UP" to 103,
            "KEY_PAGEUP" to 104,
            "KEY_LEFT" to 105,
            "KEY_RIGHT" to 106,
            "KEY_END" to 107,
            "KEY_DOWN" to 108,
            "KEY_PAGEDOWN" to 109,
            "KEY_INSERT" to 110,
            "KEY_DELETE" to 111,
            "KEY_MACRO" to 112,
            "KEY_MUTE" to 113,
            "KEY_VOLUMEDOWN" to 114,
            "KEY_VOLUMEUP" to 115,
            "KEY_POWER" to 116,     /* SC System Power Down */
            "KEY_KPEQUAL" to 117,
            "KEY_KPPLUSMINUS" to 118,
            "KEY_PAUSE" to 119,
            "KEY_SCALE" to 120,     /* AL Compiz Scale (Expose) */

            "KEY_KPCOMMA" to 121,
            "KEY_HANGEUL" to 122,
            "KEY_HANGUEL" to 122, /* KEY_HANGUEL to KEY_HANGEUL */
            "KEY_HANJA" to 123,
            "KEY_YEN" to 124,
            "KEY_LEFTMETA" to 125,
            "KEY_RIGHTMETA" to 126,
            "KEY_COMPOSE" to 127,

            "KEY_STOP" to 128,     /* AC Stop */
            "KEY_AGAIN" to 129,
            "KEY_PROPS" to 130,     /* AC Properties */
            "KEY_UNDO" to 131,     /* AC Undo */
            "KEY_FRONT" to 132,
            "KEY_COPY" to 133,     /* AC Copy */
            "KEY_OPEN" to 134,     /* AC Open */
            "KEY_PASTE" to 135,     /* AC Paste */
            "KEY_FIND" to 136,     /* AC Search */
            "KEY_CUT" to 137,     /* AC Cut */
            "KEY_HELP" to 138,     /* AL Integrated Help Center */
            "KEY_MENU" to 139,     /* Menu (show menu) */
            "KEY_CALC" to 140,     /* AL Calculator */
            "KEY_SETUP" to 141,
            "KEY_SLEEP" to 142,     /* SC System Sleep */
            "KEY_WAKEUP" to 143,     /* System Wake Up */
            "KEY_FILE" to 144,     /* AL Local Machine Browser */
            "KEY_SENDFILE" to 145,
            "KEY_DELETEFILE" to 146,
            "KEY_XFER" to 147,
            "KEY_PROG1" to 148,
            "KEY_PROG2" to 149,
            "KEY_WWW" to 150,     /* AL Internet Browser */
            "KEY_MSDOS" to 151,
            "KEY_COFFEE" to 152,     /* AL Terminal Lock/Screensaver */
            "KEY_SCREENLOCK" to 152, /* KEY_SCREENLOCK to KEY_COFFEE */
            "KEY_ROTATE_DISPLAY" to 153,     /* Display orientation for e.g. tablets */
            "KEY_DIRECTION" to 153, /* KEY_DIRECTION KEY_ROTATE_DISPLAY */
            "KEY_CYCLEWINDOWS" to 154,
            "KEY_MAIL" to 155,
            "KEY_BOOKMARKS" to 156,     /* AC Bookmarks */
            "KEY_COMPUTER" to 157,
            "KEY_BACK" to 158,     /* AC Back */
            "KEY_FORWARD" to 159,     /* AC Forward */
            "KEY_CLOSECD" to 160,
            "KEY_EJECTCD" to 161,
            "KEY_EJECTCLOSECD" to 162,
            "KEY_NEXTSONG" to 163,
            "KEY_PLAYPAUSE" to 164,
            "KEY_PREVIOUSSONG" to 165,
            "KEY_STOPCD" to 166,
            "KEY_RECORD" to 167,
            "KEY_REWIND" to 168,
            "KEY_PHONE" to 169,     /* Media Select Telephone */
            "KEY_ISO" to 170,
            "KEY_CONFIG" to 171,     /* AL Consumer Control Configuration */
            "KEY_HOMEPAGE" to 172,     /* AC Home */
            "KEY_REFRESH" to 173,     /* AC Refresh */
            "KEY_EXIT" to 174,     /* AC Exit */
            "KEY_MOVE" to 175,
            "KEY_EDIT" to 176,
            "KEY_SCROLLUP" to 177,
            "KEY_SCROLLDOWN" to 178,
            "KEY_KPLEFTPAREN" to 179,
            "KEY_KPRIGHTPAREN" to 180,
            "KEY_NEW" to 181,     /* AC New */
            "KEY_REDO" to 182,     /* AC Redo/Repeat */

            "KEY_F13" to 183,
            "KEY_F14" to 184,
            "KEY_F15" to 185,
            "KEY_F16" to 186,
            "KEY_F17" to 187,
            "KEY_F18" to 188,
            "KEY_F19" to 189,
            "KEY_F20" to 190,
            "KEY_F21" to 191,
            "KEY_F22" to 192,
            "KEY_F23" to 193,
            "KEY_F24" to 194,

            "KEY_PLAYCD" to 200,
            "KEY_PAUSECD" to 201,
            "KEY_PROG3" to 202,
            "KEY_PROG4" to 203,
            "KEY_ALL_APPLICATIONS" to 204,     /* AC Desktop Show All Applications */
            "KEY_DASHBOARD" to 204, /* KEY_DASHBOARD to KEY_ALL_APPLICATIONS */
            "KEY_SUSPEND" to 205,
            "KEY_CLOSE" to 206,     /* AC Close */
            "KEY_PLAY" to 207,
            "KEY_FASTFORWARD" to 208,
            "KEY_BASSBOOST" to 209,
            "KEY_PRINT" to 210,     /* AC Print */
            "KEY_HP" to 211,
            "KEY_CAMERA" to 212,
            "KEY_SOUND" to 213,
            "KEY_QUESTION" to 214,
            "KEY_EMAIL" to 215,
            "KEY_CHAT" to 216,
            "KEY_SEARCH" to 217,
            "KEY_CONNECT" to 218,
            "KEY_FINANCE" to 219,     /* AL Checkbook/Finance */
            "KEY_SPORT" to 220,
            "KEY_SHOP" to 221,
            "KEY_ALTERASE" to 222,
            "KEY_CANCEL" to 223,     /* AC Cancel */
            "KEY_BRIGHTNESSDOWN" to 224,
            "KEY_BRIGHTNESSUP" to 225,
            "KEY_MEDIA" to 226,

            "KEY_SWITCHVIDEOMODE" to 227,
            /* Cycle between available video
                                                  outputs (Monitor/LCD/TV-out/etc) */
            "KEY_KBDILLUMTOGGLE" to 228,
            "KEY_KBDILLUMDOWN" to 229,
            "KEY_KBDILLUMUP" to 230,

            "KEY_SEND" to 231,     /* AC Send */
            "KEY_REPLY" to 232,     /* AC Reply */
            "KEY_FORWARDMAIL" to 233,     /* AC Forward Msg */
            "KEY_SAVE" to 234,     /* AC Save */
            "KEY_DOCUMENTS" to 235,

            "KEY_BATTERY" to 236,

            "KEY_BLUETOOTH" to 237,
            "KEY_WLAN" to 238,
            "KEY_UWB" to 239,

            "KEY_UNKNOWN" to 240,

            "KEY_VIDEO_NEXT" to 241,     /* drive next video source */
            "KEY_VIDEO_PREV" to 242,     /* drive previous video source */
            "KEY_BRIGHTNESS_CYCLE" to 243,     /* brightness up, after max is min */
            "KEY_BRIGHTNESS_AUTO" to 244,
            /* Set Auto Brightness: manual
                                                 brightness control is off,
                                                 rely on ambient */
            "KEY_BRIGHTNESS_ZERO" to 244, /* KEY_BRIGHTNESS_ZERO to KEY_BRIGHTNESS_AUTO */
            "KEY_DISPLAY_OFF" to 245,     /* display device to off state */

            "KEY_WWAN" to 246,     /* Wireless WAN (LTE, UMTS, GSM, etc.) */
            "KEY_WIMAX" to 246, /* KEY_WIMAX to KEY_WWAN */
            "KEY_RFKILL" to 247,     /* Key that controls all radios */

            "KEY_MICMUTE" to 248,     /* Mute / unmute the microphone */
        )

        // assume qwerty layout
        private val CHAR_MAP: List<Pair<String, String>> = listOf(
            "`" to "KEY_GRAVE",
            "1" to "KEY_1",
            "2" to "KEY_2",
            "3" to "KEY_3",
            "4" to "KEY_4",
            "5" to "KEY_5",
            "6" to "KEY_6",
            "7" to "KEY_7",
            "8" to "KEY_8",
            "9" to "KEY_9",
            "0" to "KEY_0",
            "-" to "KEY_MINUS",
            "=" to "KEY_EQUAL",

            "Q" to "KEY_Q",
            "W" to "KEY_W",
            "E" to "KEY_E",
            "R" to "KEY_R",
            "T" to "KEY_T",
            "Y" to "KEY_Y",
            "U" to "KEY_U",
            "I" to "KEY_I",
            "O" to "KEY_O",
            "P" to "KEY_P",
            "[" to "KEY_LEFTBRACE",
            "]" to "KEY_RIGHTBRACE",
            """\\""" to "KEY_BACKSLASH",

            "A" to "KEY_A",
            "S" to "KEY_S",
            "D" to "KEY_D",
            "F" to "KEY_F",
            "G" to "KEY_G",
            "H" to "KEY_H",
            "J" to "KEY_J",
            "K" to "KEY_K",
            "L" to "KEY_L",
            ";" to "KEY_SEMICOLON",
            """\'""" to "KEY_APOSTROPHE",

            "Z" to "KEY_Z",
            "X" to "KEY_X",
            "C" to "KEY_C",
            "V" to "KEY_V",
            "B" to "KEY_B",
            "N" to "KEY_N",
            "M" to "KEY_M",
            "," to "KEY_COMMA",
            "." to "KEY_DOT",
            "/" to "KEY_SLASH",
        )
    }

    // We don't process annotations at all
    override fun process(resolver: Resolver) = emptyList<KSAnnotated>()

    override fun finish() {
        val charToScancode = FunSpec
            .builder("charToScancode")
            .addParameter("ch", Char::class)
            .returns(Int::class)
            .addCode(
                """
                | return when (ch) {
                |     ${CHAR_MAP.joinToString(separator = "\n|     ") { (ch, name) -> "'$ch' -> ScancodeMapping.$name" }}
                |     else -> ScancodeMapping.KEY_RESERVED
                | }
                """.trimMargin()
            )
            .build()

        val tyScancodeMapping = TypeSpec
            .objectBuilder("ScancodeMapping")
            .apply {
                SCANCODE_MAP.forEach { (name, code) ->
                    addProperty(
                        PropertySpec
                            .builder(name, Int::class, KModifier.CONST)
                            .initializer("$code")
                            .build()
                    )
                }
            }
            .addFunction(charToScancode)
            .build()

        val file = FileSpec
            .builder("org.fcitx.fcitx5.android.core", "ScancodeMapping")
            .addType(tyScancodeMapping)
            .build()
        file.writeTo(environment.codeGenerator, false)
    }
}

class GenScancodeMappingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        GenScancodeMappingProcessor(environment)

}
