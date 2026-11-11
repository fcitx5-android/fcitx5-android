/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.allViews
import androidx.core.view.children
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.InputView
import org.fcitx.fcitx5.android.input.keyboard.KeyboardHeightPercentBase
import org.fcitx.fcitx5.android.input.keyboard.TextKeyboard
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 36], application = FcitxApplication::class)
class InputViewHeightTest {
    private lateinit var context: Context

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        AppPrefs.getInstance().keyboard.keyboardHeightPercent.sharedPreferences.edit().clear().commit()
        ThemeManager.prefs.keyBorder.setValue(false)
    }

    private fun rows(keyboard: TextKeyboard) = keyboard.children.map { Rect(it.left, it.top, it.right, it.bottom) }.toList()

    private fun fullInputView(): InputView {
        val service = Robolectric.buildService(FcitxInputMethodService::class.java).get()
        val entry = InputMethodEntry("keyboard-us", "English", "", "English", "", "en", "keyboard", false)
        val api = Proxy.newProxyInstance(FcitxAPI::class.java.classLoader, arrayOf(FcitxAPI::class.java)) { _, method, _ ->
            when (method.name) {
                "getInputMethodEntryCached" -> entry
                "getStatusAreaActionsCached" -> emptyArray<org.fcitx.fcitx5.android.core.Action>()
                "getClientPreeditCached" -> org.fcitx.fcitx5.android.core.FormattedText.Empty
                "getInputPanelCached" -> org.fcitx.fcitx5.android.core.FcitxEvent.InputPanelEvent.Data()
                else -> error("Unexpected engine access: ${method.name}")
            }
        } as FcitxAPI
        val connection = object : FcitxConnection {
            override val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            override fun <T> runImmediately(block: suspend FcitxAPI.() -> T): T = runBlocking { block(api) }
            override suspend fun <T> runOnReady(block: suspend FcitxAPI.() -> T): T = awaitCancellation()
            override fun runIfReady(block: suspend FcitxAPI.() -> Unit) = Unit
        }
        return InputView(service, connection, ThemePreset.PixelDark)
    }

    @Test fun fullImeRefreshesHeightBeforeFirstMeasure() {
        val prefs = AppPrefs.getInstance()
        prefs.keyboard.keyboardHeightPercent.setValue(30)
        prefs.advanced.keyboardHeightPercentBase.setValue(KeyboardHeightPercentBase.DisplayMetrics)
        val metrics = context.resources.displayMetrics
        metrics.heightPixels = 800
        val input = fullInputView()
        // The service is created before the final display configuration is available.
        input.resources.displayMetrics.heightPixels = 2400
        input.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY))
        input.layout(0, 0, 1080, 2400)
        val window = input.findViewById<View>(R.id.input_window)
        assertEquals(720, window.layoutParams.height)
        assertEquals(720, window.height)
        val keyboard = input.allViews.filterIsInstance<TextKeyboard>().single()
        assertEquals(720, keyboard.height)
        rows(keyboard).forEach { assertEquals(180, it.height()) }
    }

    @Test fun openingImeAfterEarlyLayoutRefreshesSavedHeight() {
        val prefs = AppPrefs.getInstance()
        prefs.keyboard.keyboardHeightPercent.setValue(30)
        prefs.advanced.keyboardHeightPercentBase.setValue(KeyboardHeightPercentBase.DisplayMetrics)
        context.resources.displayMetrics.heightPixels = 800
        val input = fullInputView()
        input.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        input.layout(0, 0, 1080, 800)
        val window = input.findViewById<View>(R.id.input_window)
        assertEquals(240, window.height)
        input.resources.displayMetrics.heightPixels = 2400
        input.startInput(EditorInfo().apply { inputType = android.text.InputType.TYPE_CLASS_TEXT },
            org.fcitx.fcitx5.android.core.CapabilityFlags.DefaultFlags)
        assertEquals(720, window.layoutParams.height)
        input.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY))
        input.layout(0, 0, 1080, 2400)
        assertEquals(720, window.height)
        val keyboard = input.allViews.filterIsInstance<TextKeyboard>().single()
        assertEquals(720, keyboard.height)
        rows(keyboard).forEach { assertEquals(180, it.height()) }
    }


    @Test @Config(qualifiers = "land-night")
    fun heightTracksPortraitMetricsAfterLandscapeNightMode() {
        val prefs = AppPrefs.getInstance()
        prefs.keyboard.keyboardHeightPercent.setValue(27)
        prefs.keyboard.keyboardHeightPercentLandscape.setValue(43)
        prefs.advanced.keyboardHeightPercentBase.setValue(KeyboardHeightPercentBase.DisplayMetrics)
        context.resources.displayMetrics.heightPixels = 1080
        val input = fullInputView()
        fun measure(width: Int, height: Int) {
            input.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            input.layout(0, 0, width, height)
        }
        measure(2400, 1080)
        val window = input.findViewById<View>(R.id.input_window)
        assertEquals(464, window.height)
        RuntimeEnvironment.setQualifiers("port-notnight")
        input.resources.displayMetrics.heightPixels = 2400
        measure(1080, 2400)
        assertEquals(648, window.layoutParams.height)
        assertEquals(648, window.height)
    }
}
