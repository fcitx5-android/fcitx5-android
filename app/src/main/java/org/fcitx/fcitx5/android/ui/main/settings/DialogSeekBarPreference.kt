/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.setOnChangeListener
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.views.dsl.core.*
import splitties.views.gravityHorizontalCenter
import splitties.views.textAppearance

/**
 * Custom preference which represents a seek bar which shows the current value in the summary. The
 * value can be changed by clicking on the preference, which brings up a dialog which a seek bar.
 * This implementation also allows for a min / max step value, while being backwards compatible.
 *
 * @property min The minimum value of the seek bar. Must not be greater or equal than [max].
 * @property max The maximum value of the seek bar. Must not be lesser or equal than [min].
 * @property step The step in which the seek bar increases per move. If the provided value is less
 *  than 1, 1 will be used as step.
 * @property unit The unit to show after the value. Set to an empty string to disable this feature.
 */
class DialogSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : DialogPreference(context, attrs, defStyleAttr) {

    private var value = 0
    var min: Int
    var max: Int
    var step: Int
    var unit: String

    var default: Int? = null
    var defaultLabel: String? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.DialogSeekBarPreference, 0, 0).run {
            try {
                min = getInteger(R.styleable.DialogSeekBarPreference_min, 0)
                max = getInteger(R.styleable.DialogSeekBarPreference_max, 100)
                step = getInteger(R.styleable.DialogSeekBarPreference_step, 1)
                unit = getString(R.styleable.DialogSeekBarPreference_unit) ?: ""
                if (getBoolean(R.styleable.DialogSeekBarPreference_useSimpleSummaryProvider, false)) {
                    summaryProvider = SimpleSummaryProvider
                }
            } finally {
                recycle()
            }
        }
    }

    override fun persistInt(value: Int): Boolean {
        return super.persistInt(value).also {
            if (it) this@DialogSeekBarPreference.value = value
        }
    }

    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
        (defaultValue as? Int)?.let { default = it }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, 0)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedInt(defaultValue as? Int ?: 0)
    }

    override fun onClick() {
        showSeekBarDialog()
    }

    /**
     * Shows the seek bar dialog.
     */
    private fun showSeekBarDialog() {
        val textView = context.textView {
            text = textForValue(value)
            textAppearance = context.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
        }
        val seekBar = context.seekBar {
            max = progressForValue(this@DialogSeekBarPreference.max)
            progress = progressForValue(value)
            setOnChangeListener {
                textView.text = textForValue(valueForProgress(it))
            }
        }
        val dialogContent = context.verticalLayout {
            gravity = gravityHorizontalCenter
            if (dialogMessage != null) {
                val messageText = textView { text = dialogMessage }
                add(messageText, lParams {
                    verticalMargin = dp(8)
                    horizontalMargin = dp(24)
                })
            }
            add(textView, lParams {
                verticalMargin = dp(24)
            })
            add(seekBar, lParams {
                width = matchParent
                horizontalMargin = dp(10)
                bottomMargin = dp(10)
            })
        }
        AlertDialog.Builder(context)
            .setTitle(this@DialogSeekBarPreference.dialogTitle)
            .setView(dialogContent)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = valueForProgress(seekBar.progress)
                setValue(value)
            }
            .setNeutralButton(R.string.default_) { _, _ ->
                default?.let {
                    setValue(it)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setValue(value: Int) {
        if (callChangeListener(value)) {
            persistInt(value)
            notifyChanged()
        }
    }

    /**
     * Converts the actual value to a progress value which the Android SeekBar implementation can
     * handle. (Android's SeekBar step is fixed at 1 and min at 0)
     *
     * @param value The actual value.
     * @return the internal value which is used to allow different min and step values.
     */
    private fun progressForValue(value: Int) = (value - min) / step

    /**
     * Converts the Android SeekBar value to the actual value.
     *
     * @param progress The progress value of the SeekBar.
     * @return the actual value which is ready to use.
     */
    private fun valueForProgress(progress: Int) = (progress * step) + min

    private fun textForValue(value: Int = this@DialogSeekBarPreference.value): String =
        if (value == default && defaultLabel != null) defaultLabel!! else "$value $unit"

    object SimpleSummaryProvider : SummaryProvider<DialogSeekBarPreference> {
        override fun provideSummary(preference: DialogSeekBarPreference): CharSequence {
            return preference.textForValue()
        }
    }
}
