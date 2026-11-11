/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.allViews
import androidx.core.view.children
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.input.keyboard.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = FcitxApplication::class)
class ShuangpinSemicolonTest {
    private lateinit var context: Context

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        AppPrefs.getInstance().keyboard.keepLettersUppercase.sharedPreferences.edit().clear().commit()
        ThemeManager.prefs.keyBorder.setValue(false)
    }

    private fun keyboard() = TextKeyboard(context, ThemePreset.PixelDark).also { it.onAttach() }
    private fun keys(keyboard: TextKeyboard, row: Int) =
        (keyboard.getChildAt(row) as ViewGroup).children.filterIsInstance<KeyView>().toList()
    private fun layout(keyboard: TextKeyboard) {
        keyboard.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
        keyboard.layout(0, 0, 1080, 600)
    }

    @Test fun requiredKeyAlignsBothRowsAndUnusedProfilesRestoreOriginalRow() {
        val keyboard = keyboard()
        for (metadata in listOf("abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz;", ";", "", "abc")) {
            keyboard.setShuangpinInputKeys(metadata)
            layout(keyboard)
            val required = ';' in metadata
            val second = keys(keyboard, 1)
            assertEquals(if (required) 10 else 9, second.size)
            val semicolon = keyboard.findViewById<TextKeyView>(R.id.button_semicolon)
            if (required) {
                assertEquals(";", semicolon.mainText.text.toString())
                keys(keyboard, 0).zip(second).forEach { (upper, lower) ->
                    assertEquals(upper.left, lower.left)
                    assertEquals(upper.right, lower.right)
                }
                val parent = semicolon.mainText.parent as View
                assertTrue(kotlin.math.abs(semicolon.mainText.top + semicolon.mainText.bottom - parent.height) <= 1)
            } else assertNull(semicolon)
        }
    }

    @Test fun semicolonSendsNativeVirtualCodeWithoutShiftAndPreservesPeriod() {
        for (caps in 0..2) {
            val keyboard = keyboard()
            keyboard.setShuangpinInputKeys("abcdefghijklmnopqrstuvwxyz;")
            layout(keyboard)
            if (caps == 1) keyboard.caps.performClick()
            if (caps == 2) keyboard.caps.performLongClick()
            val events = mutableListOf<KeyAction>()
            keyboard.keyActionListener = KeyActionListener { action, _ -> events += action }
            keyboard.findViewById<View>(R.id.button_semicolon).performClick()
            val action = events.single() as KeyAction.SymAction
            assertEquals(FcitxKeyMapping.FcitxKey_semicolon, action.sym.sym)
            assertEquals(KeyStates.Virtual, action.states)
            events.clear()
            val period = keyboard.allViews.filterIsInstance<TextKeyView>().single {
                (it.def as KeyDef.Appearance.Text).displayText == "."
            }
            assertEquals(".", period.mainText.text.toString())
            period.performClick()
            assertEquals(".", (events.single() as KeyAction.FcitxKeyAction).act)
        }
    }

    @Test fun rebuiltRowRetainsCapsUpdatesAndLanguageSwitchRemovesExtraKey() {
        val keyboard = keyboard()
        keyboard.caps.performClick()
        keyboard.setShuangpinInputKeys("abcdefghijklmnopqrstuvwxyz;")
        assertEquals("A", (keys(keyboard, 1).first() as TextKeyView).mainText.text.toString())
        val currentA = keys(keyboard, 1).first()
        keyboard.setShuangpinInputKeys(";")
        assertSame(currentA, keys(keyboard, 1).first())
        keyboard.setShuangpinInputKeys("abcdefghijklmnopqrstuvwxyz")
        assertEquals("A", (keys(keyboard, 1).first() as TextKeyView).mainText.text.toString())
        keyboard.caps.performClick()
        assertEquals("a", (keys(keyboard, 1).first() as TextKeyView).mainText.text.toString())
        keyboard.setShuangpinInputKeys("abcdefghijklmnopqrstuvwxyz;")
        keyboard.onInputMethodUpdate(InputMethodEntry("keyboard-us", "English", "", "English", "", "en", "keyboard", false))
        layout(keyboard)
        assertEquals(9, keys(keyboard, 1).size)
        assertNull(keyboard.findViewById<View>(R.id.button_semicolon))
    }
}
