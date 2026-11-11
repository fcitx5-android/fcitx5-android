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
import org.fcitx.fcitx5.android.core.InputMethodEntry
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
@Config(sdk = [28, 36], application = FcitxApplication::class)
class ShuangpinHintsTest {
    private lateinit var context: Context

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        AppPrefs.getInstance().keyboard.showShuangpinHints.sharedPreferences.edit().clear().commit()
        ThemeManager.prefs.keyBorder.setValue(false)
    }

    private fun keyboard() = TextKeyboard(context, ThemePreset.PixelDark).also {
        it.onAttach()
        it.onInputMethodUpdate(InputMethodEntry("keyboard-us", "English", "", "English", "", "en", "keyboard", false))
    }

    private fun layout(keyboard: TextKeyboard) {
        keyboard.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
        keyboard.layout(0, 0, 1080, 600)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun hintModeShrinksCenterLabelsAndRestoresMetrics() {
        val prefs = AppPrefs.getInstance().keyboard
        prefs.showShuangpinHints.setValue(true)
        val keyboard = keyboard()
        keyboard.setShuangpinHints(arrayOf(";", "", "ing", "a", "ch", ""))
        layout(keyboard)
        val centers = keyboard.allViews.filterIsInstance<TextKeyView>().filter {
            val text = (it.def as KeyDef.Appearance.Text).displayText
            text.length == 1 && (text[0].isLetter() || text == ";")
        }.toList()
        assertEquals(26, centers.size)
        val density = context.resources.displayMetrics.density
        centers.forEach {
            assertEquals(20f * density, it.mainText.textSize, 0.01f)
            val parent = it.mainText.parent as View
            assertTrue(kotlin.math.abs(it.mainText.left + it.mainText.right - parent.width) <= 1)
            assertTrue(kotlin.math.abs(it.mainText.top + it.mainText.bottom - parent.height) <= 1)
        }
        val a = centers.first { (it.def as KeyDef.Appearance.Text).displayText == "A" }
        val smallWidth = a.mainText.measuredWidth
        val smallHeight = a.mainText.measuredHeight
        prefs.showShuangpinHints.setValue(false)
        keyboard.onAttach()
        layout(keyboard)
        centers.forEach { assertEquals(23f * density, it.mainText.textSize, 0.01f) }
        assertTrue("width ${a.mainText.measuredWidth} after $smallWidth", a.mainText.measuredWidth > smallWidth)
        assertTrue("height ${a.mainText.measuredHeight} after $smallHeight", a.mainText.measuredHeight > smallHeight)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun joinedHintsMatchNativeLetterGeometryAndRestoreSplitMode() {
        val prefs = AppPrefs.getInstance().keyboard
        val layoutPref = prefs.shuangpinHintLayout
        assertEquals(ShuangpinHintLayout.Split, layoutPref.getValue())
        ThemeManager.prefs.punctuationPosition.setValue(
            org.fcitx.fcitx5.android.data.theme.ThemePrefs.PunctuationPosition.Bottom)
        val keyboard = keyboard()
        layout(keyboard)
        fun letters() = keyboard.allViews.filterIsInstance<org.fcitx.fcitx5.android.input.keyboard.AltTextKeyView>()
            .filter { (it.def as KeyDef.Appearance.AltText).displayText.let { s ->
                s.length == 1 && (s[0].isLetter() || s == ";")
            } }.toList()
        val native = letters().first { (it.def as KeyDef.Appearance.Text).displayText == "A" }
        val nativeTop = native.mainText.top
        val nativeBottom = native.mainText.bottom
        val nativeSize = native.mainText.textSize
        keyboard.setShuangpinHints(arrayOf("a", "ch", "ia/ua", ";", "", "ing"))
        layout(keyboard)
        fun visibleHints(key: org.fcitx.fcitx5.android.input.keyboard.AltTextKeyView) =
            (key.mainText.parent as ViewGroup).children
                .filterIsInstance<org.fcitx.fcitx5.android.input.AutoScaleTextView>()
                .filter { it !== key.mainText && it !== key.altText && it.visibility == View.VISIBLE }.toList()
        val originalA = letters().first { (it.def as KeyDef.Appearance.Text).displayText == "A" }
        val originalHints = visibleHints(originalA)
        val originalFont = originalHints.first().typeface
        val originalHintSize = originalHints.first().textSize
        val originalTop = originalHints.first().top
        val originalBottom = originalHints.last().bottom
        val rowHeight = letters().first().height
        val density = context.resources.displayMetrics.density
        for (mode in listOf(ShuangpinHintLayout.Below, ShuangpinHintLayout.Above, ShuangpinHintLayout.Split,
            ShuangpinHintLayout.Above, ShuangpinHintLayout.Below)) {
            layoutPref.setValue(mode)
            keyboard.onAttach()
            layout(keyboard)
            val keys = letters()
            assertEquals(26, keys.size)
            val a = keys.first { (it.def as KeyDef.Appearance.Text).displayText == "A" }
            val noHint = keys.first { (it.def as KeyDef.Appearance.Text).displayText == "B" }
            if (mode == ShuangpinHintLayout.Split) {
                keys.forEach {
                    assertEquals(20f * density, it.mainText.textSize, 0.01f)
                    assertTrue(kotlin.math.abs(it.mainText.top + it.mainText.bottom - (it.mainText.parent as View).height) <= 1)
                    assertEquals(View.GONE, it.altText.visibility)
                }
            } else {
                val combinedHint = visibleHints(a).single()
                assertEquals("ch/ia/ua", combinedHint.text.toString())
                assertEquals(originalFont, combinedHint.typeface)
                assertEquals(originalHintSize, combinedHint.textSize, 0.01f)
                assertEquals(android.view.Gravity.CENTER, combinedHint.gravity)
                assertTrue(kotlin.math.abs(combinedHint.left + combinedHint.right - (combinedHint.parent as View).width) <= 1)
                assertTrue(visibleHints(noHint).isEmpty())
                assertEquals(View.INVISIBLE, a.altText.visibility)
                if (mode == ShuangpinHintLayout.Above) assertEquals(originalTop, combinedHint.top)
                else assertEquals(originalBottom, combinedHint.bottom)
                keys.forEach {
                    assertEquals(nativeSize, it.mainText.textSize, 0.01f)
                    if (mode == ShuangpinHintLayout.Below) {
                        assertEquals(nativeTop, it.mainText.top)
                        assertEquals(nativeBottom, it.mainText.bottom)
                        assertTrue(it.mainText.bottom <= it.altText.top)
                    } else {
                        assertTrue(kotlin.math.abs(rowHeight - nativeBottom - kotlin.math.round(2f * density).toInt() - it.mainText.top) <= 1)
                        assertTrue(kotlin.math.abs(rowHeight - nativeTop - kotlin.math.round(2f * density).toInt() - it.mainText.bottom) <= 1)
                        assertTrue(it.altText.bottom <= it.mainText.top)
                    }
                }
            }
        }
        val a = letters().first { (it.def as KeyDef.Appearance.Text).displayText == "A" }
        prefs.showShuangpinHints.setValue(false)
        keyboard.onAttach()
        layout(keyboard)
        assertEquals(nativeTop, a.mainText.top)
        assertEquals(nativeSize, a.mainText.textSize, 0.01f)
        assertEquals("@", a.altText.text.toString())
        prefs.showShuangpinHints.setValue(true)
        keyboard.setShuangpinHints(emptyArray())
        layout(keyboard)
        assertEquals(26, letters().size)
        assertEquals(nativeTop, a.mainText.top)
        assertEquals("@", a.altText.text.toString())
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun mixedHintLayoutsPreserveSymbolsAndKeepMainLabelsCentered() {
        val prefs = AppPrefs.getInstance().keyboard
        val keyboard = keyboard()
        keyboard.setShuangpinHints(arrayOf("a", "ch", "ia/ua", ";", "", "ing"))
        layout(keyboard)
        val keys = keyboard.allViews.filterIsInstance<org.fcitx.fcitx5.android.input.keyboard.AltTextKeyView>()
            .filter { (it.def as KeyDef.Appearance.AltText).displayText.let { text ->
                text.length == 1 && (text[0].isLetter() || text == ";")
            } }.toList()
        val a = keys.first { (it.def as KeyDef.Appearance.Text).displayText == "A" }
        fun hints(key: org.fcitx.fcitx5.android.input.keyboard.AltTextKeyView) =
            (key.mainText.parent as ViewGroup).children
                .filterIsInstance<org.fcitx.fcitx5.android.input.AutoScaleTextView>()
                .filter { it !== key.mainText && it !== key.altText && it.visibility == View.VISIBLE }.toList()
        val originalHint = hints(a).first()
        val originalFont = originalHint.typeface
        val originalSize = originalHint.textSize
        val hintTop = originalHint.top
        val hintBottom = hints(a).last().bottom
        val symbolFont = a.altText.typeface
        val symbolSize = a.altText.textSize
        val density = context.resources.displayMetrics.density
        for (mode in listOf(ShuangpinHintLayout.AboveWithSymbols, ShuangpinHintLayout.BelowWithSymbols,
            ShuangpinHintLayout.Above, ShuangpinHintLayout.BelowWithSymbols, ShuangpinHintLayout.AboveWithSymbols)) {
            prefs.shuangpinHintLayout.setValue(mode)
            keyboard.onAttach()
            keyboard.onPunctuationUpdate(mapOf("@" to "※"))
            layout(keyboard)
            if (mode == ShuangpinHintLayout.Above) {
                assertEquals(View.INVISIBLE, a.altText.visibility)
                continue
            }
            val hint = hints(a).single()
            assertEquals("ch/ia/ua", hint.text.toString())
            assertEquals(originalFont, hint.typeface)
            assertEquals(originalSize, hint.textSize, 0.01f)
            assertEquals("※", a.altText.text.toString())
            assertEquals(View.VISIBLE, a.altText.visibility)
            assertEquals(symbolFont, a.altText.typeface)
            assertEquals(symbolSize, a.altText.textSize, 0.01f)
            keys.forEach {
                val parent = it.mainText.parent as View
                assertEquals(20f * density, it.mainText.textSize, 0.01f)
                assertTrue(kotlin.math.abs(it.mainText.top + it.mainText.bottom - parent.height) <= 1)
                assertTrue(kotlin.math.abs(it.mainText.left + it.mainText.right - parent.width) <= 1)
            }
            assertTrue(kotlin.math.abs(a.altText.left + a.altText.right - (a.altText.parent as View).width) <= 1)
            if (mode == ShuangpinHintLayout.AboveWithSymbols) {
                assertEquals(hintTop, hint.top)
                assertTrue(hint.bottom <= a.mainText.top)
                assertTrue(a.mainText.bottom <= a.altText.top)
            } else {
                assertEquals(hintBottom, hint.bottom)
                assertTrue(a.altText.bottom <= a.mainText.top)
                assertTrue(a.mainText.bottom <= hint.top)
            }
        }
        prefs.showShuangpinHints.setValue(false)
        keyboard.onAttach()
        layout(keyboard)
        assertTrue(hints(a).isEmpty())
        assertEquals("※", a.altText.text.toString())
        assertEquals(23f * density, a.mainText.textSize, 0.01f)
    }
}
