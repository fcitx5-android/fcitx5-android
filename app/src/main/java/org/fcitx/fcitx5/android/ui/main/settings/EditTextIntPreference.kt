package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.content.res.TypedArray
import android.text.InputType
import android.text.method.DigitsKeyListener
import androidx.preference.EditTextPreference

class EditTextIntPreference(context: Context) : EditTextPreference(context) {

    private var value = 0
    var min: Int = 0
    var max: Int = Int.MAX_VALUE
    var unit: String = ""

    private val currentValue: Int
        get() = getPersistedInt(value)

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, 0)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = defaultValue as? Int ?: getPersistedInt(0)
    }

    init {
        setOnBindEditTextListener {
            it.setText(currentValue.toString())
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.keyListener = DigitsKeyListener.getInstance("0123456789")
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
            return preference.run { "$currentValue $unit" }
        }
    }
}
