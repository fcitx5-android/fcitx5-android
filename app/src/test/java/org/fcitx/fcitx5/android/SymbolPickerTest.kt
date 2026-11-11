/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.allViews
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.data.RecentlyUsed
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.input.keyboard.*
import org.fcitx.fcitx5.android.input.picker.*
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = FcitxApplication::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class SymbolPickerTest {
    private lateinit var context: Context
    private val raw = ",.?!:;()'\"`".map(Char::toString)
    private val full = "，。？！：；（）“”、".map(Char::toString)
    private val preference get() = AppPrefs.getInstance().symbols.fullWidthSymbolPunctuation

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        preference.sharedPreferences.edit().clear().commit()
        context.getSharedPreferences(RecentlyUsed.PREFERENCE_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun page() = PickerPageUi(context, ThemePreset.PixelDark, PickerPageUi.Density.High)
    private fun keys(ui: PickerPageUi) = (ui.root as ViewGroup).children.filterIsInstance<TextKeyView>().toList()

    @Test fun labelsAndCommittedTextFollowExactMappingAndRestore() {
        assertFalse(preference.getValue())
        val policy = SymbolPickerPolicy()
        val ui = page()
        val committed = mutableListOf<String>()
        ui.keyActionListener = KeyActionListener { action, _ -> committed += (action as KeyAction.CommitAction).text }
        for (enabled in listOf(false, true, false)) {
            preference.setValue(enabled)
            ui.setItems(raw, policy)
            val expected = if (enabled) full else raw
            assertEquals(expected, keys(ui).take(raw.size).map { it.mainText.text.toString() })
            committed.clear()
            keys(ui).take(raw.size).forEach { it.performClick() }
            assertEquals(expected, committed)
        }
    }

    @Test fun previewAndLongPressUseTransformedSymbols() {
        preference.setValue(true)
        val ui = page()
        val events = mutableListOf<PopupAction>()
        ui.popupActionListener = PopupActionListener { events += it }
        ui.setItems(listOf("."), SymbolPickerPolicy())
        ui.root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
        ui.root.layout(0, 0, 1080, 600)
        val key = keys(ui).first()
        key.onGestureListener!!.onGesture(key, CustomGestureView.Event(
            CustomGestureView.GestureType.Down, false, 0f, 0f, 0, 0, 0, 0))
        assertEquals("。", events.filterIsInstance<PopupAction.PreviewAction>().single().content)
        key.performLongClick()
        val popup = events.filterIsInstance<PopupAction.ShowKeyboardAction>().single().keyboard as KeyDef.Popup.Keyboard.Explicit
        assertEquals(listOf(".", "，", "？", "！", "：", "；", "_", "%", "$", "^", "&"), popup.items.toList())
    }

    @Test fun bottomCommaAndPeriodHaveMatchingLabelsAndActions() {
        val keyboard = PickerLayout.Keyboard(context, ThemePreset.PixelDark,
            ImageLayoutSwitchKey(R.drawable.ic_number_pad, NumberKeyboard.Name))
        val policy = SymbolPickerPolicy()
        val committed = mutableListOf<String>()
        val events = mutableListOf<PopupAction>()
        keyboard.keyActionListener = KeyActionListener { action, _ ->
            committed += when (action) {
                is KeyAction.FcitxKeyAction -> action.act
                is KeyAction.CommitAction -> action.text
                else -> error("Unexpected action")
            }
        }
        keyboard.popupActionListener = PopupActionListener { action ->
            events += action
            if (action is PopupAction.TriggerAction) {
                action.outAction = KeyAction.CommitAction(".")
            }
        }
        for (enabled in listOf(false, true, false)) {
            preference.setValue(enabled)
            keyboard.updatePunctuationLabels(policy)
            val punctuation = keyboard.allViews.filterIsInstance<TextKeyView>().filter {
                (it.def as KeyDef.Appearance.Text).displayText in listOf(",", ".", "，", "。")
            }.toList()
            assertEquals(2, punctuation.size)
            val expected = if (enabled) listOf("，", "。") else listOf(",", ".")
            assertEquals(expected, punctuation.map { it.mainText.text.toString() })
            committed.clear()
            punctuation.forEach { it.performClick() }
            assertEquals(expected, committed)
            events.clear()
            punctuation.forEach { it.performLongClick() }
            val popups = events.filterIsInstance<PopupAction.ShowKeyboardAction>()
            assertEquals(if (enabled) 2 else 0, popups.size)
            if (enabled) {
                assertEquals(listOf(",", "."), popups.map {
                    (it.keyboard as KeyDef.Popup.Keyboard.Explicit).items.first()
                })
                punctuation.last().onGestureListener!!.onGesture(punctuation.last(),
                    CustomGestureView.Event(CustomGestureView.GestureType.Up, false, 0f, 0f, 0, 0, 0, 0))
                // Selecting the half-width popup must bypass the full-width press transformation.
                assertEquals(".", committed.last())
            }
        }
    }

    @Test fun everyConvertedSymbolOffersHalfWidthFirst() {
        preference.setValue(true)
        val policy = SymbolPickerPolicy()
        val ui = page()
        val events = mutableListOf<PopupAction>()
        ui.popupActionListener = PopupActionListener { events += it }
        ui.setItems(raw, policy)
        keys(ui).take(raw.size).forEach { it.performLongClick() }
        val popups = events.filterIsInstance<PopupAction.ShowKeyboardAction>()
        assertEquals(raw.size, popups.size)
        assertEquals(raw, popups.map { (it.keyboard as KeyDef.Popup.Keyboard.Explicit).items.first() })
        full.zip(raw).forEach { (wide, ascii) ->
            val popup = policy.popup(wide) as KeyDef.Popup.Keyboard.Explicit
            assertEquals(ascii, popup.items.first())
            assertEquals(popup.items.distinct(), popup.items.toList())
        }
        preference.setValue(false)
        raw.forEach { assertTrue(policy.popup(it) is KeyDef.Popup.Keyboard.Preset) }
    }

    @Test fun recentPageRefreshesWithoutRewritingStoredHistory() {
        val policy = SymbolPickerPolicy()
        val committed = mutableListOf<String>()
        val listener = KeyActionListener { action, _ -> committed += (action as KeyAction.CommitAction).text }
        val adapter = PickerPagesAdapter(ThemePreset.PixelDark, listener, PopupActionListener {},
            listOf(PickerData.Category("test") to raw.toTypedArray()), PickerPageUi.Density.High,
            "symbol-page-test", false, policy)
        adapter.insertRecent("?")
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        holder.ui.keyActionListener = listener
        var changes = 0
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() { changes++ }
        })
        for (enabled in listOf(false, true, false)) {
            preference.setValue(enabled)
            adapter.refreshIfNeeded()
            adapter.onBindViewHolder(holder, 0)
            val expected = if (enabled) "？" else "?"
            assertEquals(expected, keys(holder.ui).first().mainText.text.toString())
            keys(holder.ui).first().performClick()
            assertEquals(expected, committed.last())
            assertEquals(listOf("?"), RecentlyUsed("symbol-page-test", 28).items)
        }
        assertEquals(2, changes)
    }

    @Test fun otherPickersAndUnspecifiedStringsKeepTheirText() {
        preference.setValue(true)
        assertTrue(ReflectionHelpers.getField<PickerPolicy>(symbolPicker(), "policy") is SymbolPickerPolicy)
        assertTrue(ReflectionHelpers.getField<PickerPolicy>(emojiPicker(), "policy") is EmojiPickerPolicy)
        assertTrue(ReflectionHelpers.getField<PickerPolicy>(emoticonPicker(), "policy") is DefaultPickerPolicy)
        val policy = SymbolPickerPolicy()
        for (text in listOf("@", "#", "/", "[]", ":-)", "https://", ".com", "‘", "’", "“", "”", "🙂")) {
            assertEquals(text, policy.transform(text))
        }
        for (text in raw) {
            assertEquals(text, DefaultPickerPolicy().transform(text))
            assertEquals(text, DefaultPickerPolicy().transformRecent(text))
        }
    }
}
