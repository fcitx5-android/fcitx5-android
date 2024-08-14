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

    data class Scancode(val name: String, val value: Int)
    data class Mapping(val scancode: Scancode, val androidKey: String)

    companion object {
        // https://github.com/torvalds/linux/blob/v6.10/include/uapi/linux/input-event-codes.h#L75
        private val SCANCODE_MAP = listOf(
            "KEY_RESERVED" to 0 to "KEYCODE_UNKNOWN",
            "KEY_ESC" to 1 to "KEYCODE_ESCAPE",
            "KEY_1" to 2 to "KEYCODE_1",
            "KEY_2" to 3 to "KEYCODE_2",
            "KEY_3" to 4 to "KEYCODE_3",
            "KEY_4" to 5 to "KEYCODE_4",
            "KEY_5" to 6 to "KEYCODE_5",
            "KEY_6" to 7 to "KEYCODE_6",
            "KEY_7" to 8 to "KEYCODE_7",
            "KEY_8" to 9 to "KEYCODE_8",
            "KEY_9" to 10 to "KEYCODE_9",
            "KEY_0" to 11 to "KEYCODE_0",
            "KEY_MINUS" to 12 to "KEYCODE_MINUS",
            "KEY_EQUAL" to 13 to "KEYCODE_EQUALS",
            "KEY_BACKSPACE" to 14 to "KEYCODE_DEL",
            "KEY_TAB" to 15 to "KEYCODE_TAB",
            "KEY_Q" to 16 to "KEYCODE_Q",
            "KEY_W" to 17 to "KEYCODE_W",
            "KEY_E" to 18 to "KEYCODE_E",
            "KEY_R" to 19 to "KEYCODE_R",
            "KEY_T" to 20 to "KEYCODE_T",
            "KEY_Y" to 21 to "KEYCODE_Y",
            "KEY_U" to 22 to "KEYCODE_U",
            "KEY_I" to 23 to "KEYCODE_I",
            "KEY_O" to 24 to "KEYCODE_O",
            "KEY_P" to 25 to "KEYCODE_P",
            "KEY_LEFTBRACE" to 26 to "KEYCODE_LEFT_BRACKET",
            "KEY_RIGHTBRACE" to 27 to "KEYCODE_RIGHT_BRACKET",
            "KEY_ENTER" to 28 to "KEYCODE_ENTER",
            "KEY_LEFTCTRL" to 29 to "KEYCODE_CTRL_LEFT",
            "KEY_A" to 30 to "KEYCODE_A",
            "KEY_S" to 31 to "KEYCODE_S",
            "KEY_D" to 32 to "KEYCODE_D",
            "KEY_F" to 33 to "KEYCODE_F",
            "KEY_G" to 34 to "KEYCODE_G",
            "KEY_H" to 35 to "KEYCODE_H",
            "KEY_J" to 36 to "KEYCODE_J",
            "KEY_K" to 37 to "KEYCODE_K",
            "KEY_L" to 38 to "KEYCODE_L",
            "KEY_SEMICOLON" to 39 to "KEYCODE_SEMICOLON",
            "KEY_APOSTROPHE" to 40 to "KEYCODE_APOSTROPHE",
            "KEY_GRAVE" to 41 to "KEYCODE_GRAVE",
            "KEY_LEFTSHIFT" to 42 to "KEYCODE_SHIFT_LEFT",
            "KEY_BACKSLASH" to 43 to "KEYCODE_BACKSLASH",
            "KEY_Z" to 44 to "KEYCODE_Z",
            "KEY_X" to 45 to "KEYCODE_X",
            "KEY_C" to 46 to "KEYCODE_C",
            "KEY_V" to 47 to "KEYCODE_V",
            "KEY_B" to 48 to "KEYCODE_B",
            "KEY_N" to 49 to "KEYCODE_N",
            "KEY_M" to 50 to "KEYCODE_M",
            "KEY_COMMA" to 51 to "KEYCODE_COMMA",
            "KEY_DOT" to 52 to "KEYCODE_PERIOD",
            "KEY_SLASH" to 53 to "KEYCODE_SLASH",
            "KEY_RIGHTSHIFT" to 54 to "KEYCODE_SHIFT_RIGHT",
            "KEY_KPASTERISK" to 55 to "KEYCODE_NUMPAD_MULTIPLY",
            "KEY_LEFTALT" to 56 to "KEYCODE_ALT_LEFT",
            "KEY_SPACE" to 57 to "KEYCODE_SPACE",
            "KEY_CAPSLOCK" to 58 to "KEYCODE_CAPS_LOCK",
            "KEY_F1" to 59 to "KEYCODE_F1",
            "KEY_F2" to 60 to "KEYCODE_F2",
            "KEY_F3" to 61 to "KEYCODE_F3",
            "KEY_F4" to 62 to "KEYCODE_F4",
            "KEY_F5" to 63 to "KEYCODE_F5",
            "KEY_F6" to 64 to "KEYCODE_F6",
            "KEY_F7" to 65 to "KEYCODE_F7",
            "KEY_F8" to 66 to "KEYCODE_F8",
            "KEY_F9" to 67 to "KEYCODE_F9",
            "KEY_F10" to 68 to "KEYCODE_F10",
            "KEY_NUMLOCK" to 69 to "KEYCODE_NUM_LOCK",
            "KEY_SCROLLLOCK" to 70 to "KEYCODE_SCROLL_LOCK",
            "KEY_KP7" to 71 to "KEYCODE_NUMPAD_7",
            "KEY_KP8" to 72 to "KEYCODE_NUMPAD_8",
            "KEY_KP9" to 73 to "KEYCODE_NUMPAD_9",
            "KEY_KPMINUS" to 74 to "KEYCODE_NUMPAD_SUBTRACT",
            "KEY_KP4" to 75 to "KEYCODE_NUMPAD_4",
            "KEY_KP5" to 76 to "KEYCODE_NUMPAD_5",
            "KEY_KP6" to 77 to "KEYCODE_NUMPAD_6",
            "KEY_KPPLUS" to 78 to "KEYCODE_NUMPAD_ADD",
            "KEY_KP1" to 79 to "KEYCODE_NUMPAD_1",
            "KEY_KP2" to 80 to "KEYCODE_NUMPAD_2",
            "KEY_KP3" to 81 to "KEYCODE_NUMPAD_3",
            "KEY_KP0" to 82 to "KEYCODE_NUMPAD_0",
            "KEY_KPDOT" to 83 to "KEYCODE_NUMPAD_DOT",

            "KEY_ZENKAKUHANKAKU" to 85 to "KEYCODE_ZENKAKU_HANKAKU",
            "KEY_102ND" to 86 to "",
            "KEY_F11" to 87 to "KEYCODE_F11",
            "KEY_F12" to 88 to "KEYCODE_F12",
            "KEY_RO" to 89 to "KEYCODE_RO",
            "KEY_KATAKANA" to 90 to "",
            "KEY_HIRAGANA" to 91 to "",
            "KEY_HENKAN" to 92 to "KEYCODE_HENKAN",
            "KEY_KATAKANAHIRAGANA" to 93 to "KEYCODE_KATAKANA_HIRAGANA",
            "KEY_MUHENKAN" to 94 to "KEYCODE_MUHENKAN",
            "KEY_KPJPCOMMA" to 95 to "",
            "KEY_KPENTER" to 96 to "KEYCODE_NUMPAD_ENTER",
            "KEY_RIGHTCTRL" to 97 to "KEYCODE_CTRL_RIGHT",
            "KEY_KPSLASH" to 98 to "KEYCODE_NUMPAD_DIVIDE",
            "KEY_SYSRQ" to 99 to "KEYCODE_SYSRQ",
            "KEY_RIGHTALT" to 100 to "KEYCODE_ALT_RIGHT",
            "KEY_LINEFEED" to 101 to "",
            "KEY_HOME" to 102 to "KEYCODE_MOVE_HOME",
            "KEY_UP" to 103 to "KEYCODE_DPAD_UP",
            "KEY_PAGEUP" to 104 to "KEYCODE_PAGE_UP",
            "KEY_LEFT" to 105 to "KEYCODE_DPAD_LEFT",
            "KEY_RIGHT" to 106 to "KEYCODE_DPAD_RIGHT",
            "KEY_END" to 107 to "KEYCODE_MOVE_END",
            "KEY_DOWN" to 108 to "KEYCODE_DPAD_DOWN",
            "KEY_PAGEDOWN" to 109 to "KEYCODE_PAGE_DOWN",
            "KEY_INSERT" to 110 to "KEYCODE_INSERT",
            "KEY_DELETE" to 111 to "KEYCODE_FORWARD_DEL",
            "KEY_MACRO" to 112 to "",
            "KEY_MUTE" to 113 to "KEYCODE_VOLUME_MUTE",
            "KEY_VOLUMEDOWN" to 114 to "KEYCODE_VOLUME_DOWN",
            "KEY_VOLUMEUP" to 115 to "KEYCODE_VOLUME_UP",
            "KEY_POWER" to 116 to "KEYCODE_POWER",     /* SC System Power Down */
            "KEY_KPEQUAL" to 117 to "KEYCODE_NUMPAD_EQUALS",
            "KEY_KPPLUSMINUS" to 118 to "",
            "KEY_PAUSE" to 119 to "KEYCODE_BREAK",
            "KEY_SCALE" to 120 to "",     /* AL Compiz Scale (Expose) */

            "KEY_KPCOMMA" to 121 to "KEYCODE_NUMPAD_COMMA",
            "KEY_HANGEUL" to 122 to "",
            "KEY_HANGUEL" to 122 to "", /* KEY_HANGUEL to KEY_HANGEUL */
            "KEY_HANJA" to 123 to "",
            "KEY_YEN" to 124 to "KEYCODE_YEN",
            "KEY_LEFTMETA" to 125 to "KEYCODE_META_LEFT",
            "KEY_RIGHTMETA" to 126 to "KEYCODE_META_RIGHT",
            "KEY_COMPOSE" to 127 to "",

            "KEY_STOP" to 128 to "",     /* AC Stop */
            "KEY_AGAIN" to 129 to "",
            "KEY_PROPS" to 130 to "",     /* AC Properties */
            "KEY_UNDO" to 131 to "",     /* AC Undo */
            "KEY_FRONT" to 132 to "",
            "KEY_COPY" to 133 to "KEYCODE_COPY",     /* AC Copy */
            "KEY_OPEN" to 134 to "",     /* AC Open */
            "KEY_PASTE" to 135 to "KEYCODE_PASTE",     /* AC Paste */
            "KEY_FIND" to 136 to "",     /* AC Search */
            "KEY_CUT" to 137 to "KEYCODE_CUT",     /* AC Cut */
            "KEY_HELP" to 138 to "KEYCODE_HELP",     /* AL Integrated Help Center */
            "KEY_MENU" to 139 to "KEYCODE_MENU",     /* Menu (show menu) */
            "KEY_CALC" to 140 to "KEYCODE_CALCULATOR",     /* AL Calculator */
            "KEY_SETUP" to 141 to "",
            "KEY_SLEEP" to 142 to "KEYCODE_SLEEP",     /* SC System Sleep */
            "KEY_WAKEUP" to 143 to "KEYCODE_WAKEUP",     /* System Wake Up */
            "KEY_FILE" to 144 to "",     /* AL Local Machine Browser */
            "KEY_SENDFILE" to 145 to "",
            "KEY_DELETEFILE" to 146 to "",
            "KEY_XFER" to 147 to "",
            "KEY_PROG1" to 148 to "",
            "KEY_PROG2" to 149 to "",
            "KEY_WWW" to 150 to "KEYCODE_EXPLORER",     /* AL Internet Browser */
            "KEY_MSDOS" to 151 to "",
            "KEY_COFFEE" to 152 to "",     /* AL Terminal Lock/Screensaver */
            "KEY_SCREENLOCK" to 152 to "", /* KEY_SCREENLOCK to KEY_COFFEE */
            "KEY_ROTATE_DISPLAY" to 153 to "",     /* Display orientation for e.g. tablets */
            "KEY_DIRECTION" to 153 to "", /* KEY_DIRECTION KEY_ROTATE_DISPLAY */
            "KEY_CYCLEWINDOWS" to 154 to "",
            "KEY_MAIL" to 155 to "KEYCODE_ENVELOPE",
            "KEY_BOOKMARKS" to 156 to "KEYCODE_BOOKMARK",     /* AC Bookmarks */
            "KEY_COMPUTER" to 157 to "",
            "KEY_BACK" to 158 to "",     /* AC Back */
            "KEY_FORWARD" to 159 to "",     /* AC Forward */
            "KEY_CLOSECD" to 160 to "",
            "KEY_EJECTCD" to 161 to "",
            "KEY_EJECTCLOSECD" to 162 to "",
            "KEY_NEXTSONG" to 163 to "KEYCODE_MEDIA_NEXT",
            "KEY_PLAYPAUSE" to 164 to "KEYCODE_MEDIA_PLAY_PAUSE",
            "KEY_PREVIOUSSONG" to 165 to "KEYCODE_MEDIA_PREVIOUS",
            "KEY_STOPCD" to 166 to "",
            "KEY_RECORD" to 167 to "",
            "KEY_REWIND" to 168 to "",
            "KEY_PHONE" to 169 to "",     /* Media Select Telephone */
            "KEY_ISO" to 170 to "",
            "KEY_CONFIG" to 171 to "",     /* AL Consumer Control Configuration */
            "KEY_HOMEPAGE" to 172 to "",     /* AC Home */
            "KEY_REFRESH" to 173 to "KEYCODE_REFRESH",     /* AC Refresh */
            "KEY_EXIT" to 174 to "",     /* AC Exit */
            "KEY_MOVE" to 175 to "",
            "KEY_EDIT" to 176 to "",
            "KEY_SCROLLUP" to 177 to "",
            "KEY_SCROLLDOWN" to 178 to "",
            "KEY_KPLEFTPAREN" to 179 to "",
            "KEY_KPRIGHTPAREN" to 180 to "",
            "KEY_NEW" to 181 to "",     /* AC New */
            "KEY_REDO" to 182 to "",     /* AC Redo/Repeat */

            "KEY_F13" to 183 to "",
            "KEY_F14" to 184 to "",
            "KEY_F15" to 185 to "",
            "KEY_F16" to 186 to "",
            "KEY_F17" to 187 to "",
            "KEY_F18" to 188 to "",
            "KEY_F19" to 189 to "",
            "KEY_F20" to 190 to "",
            "KEY_F21" to 191 to "",
            "KEY_F22" to 192 to "",
            "KEY_F23" to 193 to "",
            "KEY_F24" to 194 to "",

            "KEY_PLAYCD" to 200 to "",
            "KEY_PAUSECD" to 201 to "",
            "KEY_PROG3" to 202 to "",
            "KEY_PROG4" to 203 to "",
            "KEY_ALL_APPLICATIONS" to 204 to "",     /* AC Desktop Show All Applications */
            "KEY_DASHBOARD" to 204 to "", /* KEY_DASHBOARD to KEY_ALL_APPLICATIONS */
            "KEY_SUSPEND" to 205 to "",
            "KEY_CLOSE" to 206 to "",     /* AC Close */
            "KEY_PLAY" to 207 to "",
            "KEY_FASTFORWARD" to 208 to "",
            "KEY_BASSBOOST" to 209 to "",
            "KEY_PRINT" to 210 to "",     /* AC Print */
            "KEY_HP" to 211 to "",
            "KEY_CAMERA" to 212 to "",
            "KEY_SOUND" to 213 to "",
            "KEY_QUESTION" to 214 to "",
            "KEY_EMAIL" to 215 to "",
            "KEY_CHAT" to 216 to "",
            "KEY_SEARCH" to 217 to "KEYCODE_SEARCH",
            "KEY_CONNECT" to 218 to "",
            "KEY_FINANCE" to 219 to "",     /* AL Checkbook/Finance */
            "KEY_SPORT" to 220 to "",
            "KEY_SHOP" to 221 to "",
            "KEY_ALTERASE" to 222 to "",
            "KEY_CANCEL" to 223 to "",     /* AC Cancel */
            "KEY_BRIGHTNESSDOWN" to 224 to "",
            "KEY_BRIGHTNESSUP" to 225 to "",
            "KEY_MEDIA" to 226 to "",

            "KEY_SWITCHVIDEOMODE" to 227 to "",
            /* Cycle between available video
                                                  outputs (Monitor/LCD/TV-out/etc) */
            "KEY_KBDILLUMTOGGLE" to 228 to "",
            "KEY_KBDILLUMDOWN" to 229 to "",
            "KEY_KBDILLUMUP" to 230 to "",

            "KEY_SEND" to 231 to "",     /* AC Send */
            "KEY_REPLY" to 232 to "",     /* AC Reply */
            "KEY_FORWARDMAIL" to 233 to "",     /* AC Forward Msg */
            "KEY_SAVE" to 234 to "",     /* AC Save */
            "KEY_DOCUMENTS" to 235 to "",

            "KEY_BATTERY" to 236 to "",

            "KEY_BLUETOOTH" to 237 to "",
            "KEY_WLAN" to 238 to "",
            "KEY_UWB" to 239 to "",

            "KEY_UNKNOWN" to 240 to "",

            "KEY_VIDEO_NEXT" to 241 to "",     /* drive next video source */
            "KEY_VIDEO_PREV" to 242 to "",     /* drive previous video source */
            "KEY_BRIGHTNESS_CYCLE" to 243 to "",     /* brightness up to "", after max is min */
            "KEY_BRIGHTNESS_AUTO" to 244 to "",
            /* Set Auto Brightness: manual
                                                 brightness control is off to "",
                                                 rely on ambient */
            "KEY_BRIGHTNESS_ZERO" to 244 to "", /* KEY_BRIGHTNESS_ZERO to KEY_BRIGHTNESS_AUTO */
            "KEY_DISPLAY_OFF" to 245 to "",     /* display device to off state */

            "KEY_WWAN" to 246 to "",     /* Wireless WAN (LTE to "", UMTS to "", GSM to "", etc.) */
            "KEY_WIMAX" to 246 to "", /* KEY_WIMAX to KEY_WWAN */
            "KEY_RFKILL" to 247 to "",     /* Key that controls all radios */

            "KEY_MICMUTE" to 248 to "KEYCODE_MUTE",     /* Mute / unmute the microphone */
        ).map { Mapping(Scancode(it.first.first, it.first.second), it.second) }

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

        val keyCodeToScancode = FunSpec
            .builder("keyCodeToScancode")
            .addParameter("keyCode", Int::class)
            .returns(Int::class)
            .addCode(
                """
                | return when (keyCode) {
                |     ${
                    SCANCODE_MAP.filter { it.androidKey.isNotEmpty() }
                        .joinToString(separator = "\n|     ") { (scancode, androidKey) ->
                            "KeyEvent.${androidKey} -> ScancodeMapping.${scancode.name}"
                        }
                }
                |     else -> ScancodeMapping.KEY_RESERVED
                | }
                """.trimMargin()
            )
            .build()

        val tyScancodeMapping = TypeSpec
            .objectBuilder("ScancodeMapping")
            .apply {
                SCANCODE_MAP.forEach { (scancode, androidKey) ->
                    addProperty(
                        PropertySpec
                            .builder(scancode.name, Int::class, KModifier.CONST)
                            .initializer("${scancode.value}")
                            .build()
                    )
                }
            }
            .addFunction(charToScancode)
            .addFunction(keyCodeToScancode)
            .build()

        val file = FileSpec
            .builder("org.fcitx.fcitx5.android.core", "ScancodeMapping")
            .addImport("android.view", "KeyEvent")
            .addType(tyScancodeMapping)
            .build()
        file.writeTo(environment.codeGenerator, false)
    }
}

class GenScancodeMappingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        GenScancodeMappingProcessor(environment)

}
