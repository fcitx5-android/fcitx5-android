/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.text.InputType
import android.text.method.DigitsKeyListener
import androidx.preference.EditTextPreference
import java.util.Locale

class EditTextIntPreference(context: Context) : EditTextPreference(context) {

    private var value = 0
    var min: Int = Int.MIN_VALUE
    var max: Int = Int.MAX_VALUE
    var unit: String = ""

    private var default: Int = 0

    override fun persistInt(value: Int): Boolean {
        return super.persistInt(value).also {
            if (it) this@EditTextIntPreference.value = value
        }
    }

    // it appears as an "Int" Preference to the user, we want to accept Int for defaultValue
    override fun setDefaultValue(defaultValue: Any?) {
        val value = defaultValue as? Int ?: return
        default = value
        // the underlying Preference is an "EditText", we must use String for it's defaultValue
        super.setDefaultValue(value.toString())
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, default)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedInt(default)
    }

    private fun textForValue(): String {
        return getPersistedInt(value).toString()
    }

    init {
        setOnBindEditTextListener {
            it.setText(textForValue())
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.keyListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DigitsKeyListener.getInstance(Locale.ROOT, min < 0, false)
            } else {
                @Suppress("DEPRECATION")
                DigitsKeyListener.getInstance(min < 0, false)
            }
        }
    }

    private fun fitValue(v: Int) = if (v < min) min else if (v > max) max else v

    override fun setText(text: String?) {
        val value = text?.toIntOrNull(10) ?: return
        persistInt(fitValue(value))
        notifyChanged()
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        if (newValue !is String) return false
        val value = newValue.toIntOrNull(10) ?: return false
        return super.callChangeListener(fitValue(value))
    }

    object SimpleSummaryProvider : SummaryProvider<EditTextIntPreference> {
        override fun provideSummary(preference: EditTextIntPreference): CharSequence {
            return preference.run { "${textForValue()} $unit" }
        }
    }
}
