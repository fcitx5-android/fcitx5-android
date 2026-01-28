/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.text.InputType
import android.text.method.DigitsKeyListener
import androidx.preference.EditTextPreference
import java.util.Locale

class EditTextFloatPreference(context: Context) : EditTextPreference(context) {

    private var value = 0f
    var min: Float = -Float.MAX_VALUE
    var max: Float = Float.MAX_VALUE
    var unit: String = ""

    private var default: Float = 0f

    override fun persistFloat(value: Float): Boolean {
        return super.persistFloat(value).also {
            if (it) this@EditTextFloatPreference.value = value
        }
    }

    // it appears as a "Float" Preference to the user, we want to accept Float for defaultValue
    override fun setDefaultValue(defaultValue: Any?) {
        val value = defaultValue as? Float ?: return
        default = value
        // the underlying Preference is an "EditText", we must use String for it's defaultValue
        super.setDefaultValue(value.toString())
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getFloat(index, default)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedFloat(default)
    }

    private fun textForValue(): String {
        return getPersistedFloat(value).toString()
    }

    init {
        setOnBindEditTextListener {
            it.setText(textForValue())
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.keyListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DigitsKeyListener.getInstance(Locale.ROOT, min < 0f, true)
            } else {
                @Suppress("DEPRECATION")
                DigitsKeyListener.getInstance(min < 0f, true)
            }
        }
    }

    private fun fitValue(v: Float) = if (v < min) min else if (v > max) max else v

    override fun setText(text: String?) {
        val value = text?.toFloatOrNull() ?: return
        persistFloat(fitValue(value))
        notifyChanged()
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        if (newValue !is String) return false
        val value = newValue.toFloatOrNull() ?: return false
        return super.callChangeListener(fitValue(value))
    }

    object SimpleSummaryProvider : SummaryProvider<EditTextFloatPreference> {
        override fun provideSummary(preference: EditTextFloatPreference): CharSequence {
            return preference.run { "${textForValue()} $unit" }
        }
    }
}
