/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.data.theme.CustomThemeSerializer
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.junit.Assert
import org.junit.Test

class ThemeSerializationTest {

    private fun Theme.Custom.toJson() = Json.encodeToString(CustomThemeSerializer, this)
    private fun String.toCustomTheme() =
        Json.decodeFromString(CustomThemeSerializer.WithMigrationStatus, this)

    @Test
    fun preservation() {
        val fakeCustomTheme =
            ThemePreset
                .TransparentDark
                .deriveCustomBackground("", "", "")

        val (decoded, migrated) = fakeCustomTheme.toJson().toCustomTheme()

        Assert.assertEquals("Migration shouldn't happen", false, migrated)
        Assert.assertEquals("Versioning preserves the original structure", fakeCustomTheme, decoded)
    }

    @Test
    fun version1() {
        // Version 1.0, outdated
        val raw = """
            {
                "name": "",
                "backgroundImage": {
                    "croppedFilePath": "",
                    "srcFilePath": "",
                    "cropRect": null
                },
                "backgroundColor": -13816531,
                "barColor": 1275068416,
                "keyboardColor": 0,
                "keyBackgroundColor": 1275068415,
                "keyTextColor": -1,
                "altKeyBackgroundColor": 218103807,
                "altKeyTextColor": -905969665,
                "accentKeyBackgroundColor": -10577930,
                "accentKeyTextColor": -1,
                "keyPressHighlightColor": 520093696,
                "keyShadowColor": 0,
                "spaceBarColor": 1275068415,
                "dividerColor": 536870911,
                "clipboardEntryColor": 855638015,
                "isDark": true,
                "version": "1.0"
            }
        """.trimIndent()
        val (decoded, migrated) = raw.toCustomTheme()
        Assert.assertEquals("Migration should happen", true, migrated)
        Assert.assertEquals("Round trip", decoded, decoded.toJson().toCustomTheme().first)
    }

    @Test
    fun version2() {
        // Version 2.0
        val raw = """
            {
               "name":"",
               "backgroundImage":{
                  "croppedFilePath":"",
                  "srcFilePath":"",
                  "cropRect": null
               },
               "backgroundColor":-13816531,
               "barColor":1275068416,
               "keyboardColor":0,
               "keyBackgroundColor":1275068415,
               "keyTextColor":-1,
               "altKeyBackgroundColor":218103807,
               "altKeyTextColor":-905969665,
               "accentKeyBackgroundColor":-10577930,
               "accentKeyTextColor":-1,
               "keyPressHighlightColor":520093696,
               "keyShadowColor":0,
               "popupBackgroundColor":-13158601,
               "popupTextColor":-1,
               "spaceBarColor":1275068415,
               "dividerColor":536870911,
               "clipboardEntryColor":855638015,
               "genericActiveBackgroundColor":-10577930,
               "genericActiveForegroundColor":-1,
               "isDark":true,
               "version":"2.0"
            }
        """.trimIndent()
        val (decoded, migrated) = raw.toCustomTheme()
        Assert.assertEquals("Migration shouldn't happen", false, migrated)
        Assert.assertEquals("Round trip", decoded, decoded.toJson().toCustomTheme().first)
    }
}