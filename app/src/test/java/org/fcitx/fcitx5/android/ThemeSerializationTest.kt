package org.fcitx.fcitx5.android

import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.data.theme.CustomThemeSerializer
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ThemeSerializationTest {

    private fun Theme.Custom.toJson() = Json.encodeToString(CustomThemeSerializer, this)
    private fun String.toCustomTheme() = Json.decodeFromString(CustomThemeSerializer, this)

    private val json = Json {
        prettyPrint = true
    }

    @Test
    fun test() {
        val fakeCustomTheme =
            ThemePreset
                .TransparentDark
                .deriveCustomBackground("", "", "")

        // Versioning preserves the original structure
        Assert.assertEquals(
            fakeCustomTheme,
            fakeCustomTheme.toJson().toCustomTheme()
        )

        // Version 1.o
        val raw1 = """
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
        // No migration needed in version 1.0, just test deserialization
        raw1.toCustomTheme()

    }
}