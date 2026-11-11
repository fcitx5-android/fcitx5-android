/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import android.app.Activity
import android.app.AlertDialog
import android.view.WindowManager
import android.widget.EditText
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = FcitxApplication::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class InputMethodDialogTest {
    @Test fun pickerKeepsEditorFocusAndAllowsTouchSelection() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val editor = EditText(activity)
        activity.setContentView(editor)
        editor.requestFocus()
        // Skip the service's onCreate so this UI test does not start the native engine.
        val service = Robolectric.buildService(FcitxInputMethodService::class.java).get()
        ReflectionHelpers.setField(service, "decorView", activity.window.decorView)
        var selected = -1
        val dialog = AlertDialog.Builder(activity)
            .setItems(arrayOf("English", "双拼")) { _, index -> selected = index }
            .create()
        dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        service.showDialog(dialog)
        val flags = dialog.window!!.attributes.flags
        assertTrue(dialog.isShowing)
        assertTrue(editor.hasFocus())
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(0, flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG, dialog.window!!.attributes.type)
        dialog.listView.performItemClick(null, 1, 1)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, selected)
        assertFalse(dialog.isShowing)
        assertNull(ReflectionHelpers.getField<Any?>(service, "showingDialog"))
    }

    @Test fun replacingPickerDoesNotLoseNewDialogReferenceAndCancelStillWorks() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val service = Robolectric.buildService(FcitxInputMethodService::class.java).get()
        ReflectionHelpers.setField(service, "decorView", activity.window.decorView)
        val first = AlertDialog.Builder(activity).setMessage("First").create()
        val second = AlertDialog.Builder(activity).setMessage("Second").create()
        service.showDialog(first)
        service.showDialog(second)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertFalse(first.isShowing)
        assertTrue(second.isShowing)
        assertSame(second, ReflectionHelpers.getField<Any?>(service, "showingDialog"))
        second.cancel()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertFalse(second.isShowing)
        assertNull(ReflectionHelpers.getField<Any?>(service, "showingDialog"))
    }
}
