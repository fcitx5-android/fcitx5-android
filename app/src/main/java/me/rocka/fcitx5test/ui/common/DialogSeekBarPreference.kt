package me.rocka.fcitx5test.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.gravityHorizontalCenter

/**
 * Custom preference which represents a seek bar which shows the current value in the summary. The
 * value can be changed by clicking on the preference, which brings up a dialog which a seek bar.
 * This implementation also allows for a min / max step value, while being backwards compatible.
 *
 * @property defaultValue The default value of this preference.
 * @property min The minimum value of the seek bar. Must not be greater or equal than [max].
 * @property max The maximum value of the seek bar. Must not be lesser or equal than [min].
 * @property step The step in which the seek bar increases per move. If the provided value is less
 *  than 1, 1 will be used as step.
 * @property unit The unit to show after the value. Set to an empty string to disable this feature.
 */
class DialogSeekBarPreference : Preference {
    var defaultValue: Int = 0
    var min: Int = 0
    var max: Int = 100
    var step: Int = 1
    var unit: String = ""

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, R.attr.preferenceStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    private val currentValue: Int
        get() = preferenceDataStore?.getInt(key, defaultValue) ?: defaultValue

    override fun onClick() {
        showSeekBarDialog()
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)
        summary = textForValue(currentValue)
    }

    /**
     * Shows the seek bar dialog.
     */
    private fun showSeekBarDialog() = with(context) {
        val initValue = currentValue
        val textView = textView {
            text = textForValue(initValue)
        }
        val seekBar = seekBar {
            max = progressForValue(this@DialogSeekBarPreference.max)
            progress = progressForValue(initValue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    textView.text = textForValue(valueForProgress(progress))
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        val dialogContent = verticalLayout {
            gravity = gravityHorizontalCenter
            add(textView, lParams {
                verticalMargin = dp(16)
            })
            add(seekBar, lParams {
                width = matchParent
                horizontalMargin = dp(10)
                bottomMargin = dp(10)
            })
        }
        AlertDialog.Builder(context)
            .setTitle(this@DialogSeekBarPreference.title)
            .setView(dialogContent)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = valueForProgress(seekBar.progress)
                if (callChangeListener(value)) {
                    preferenceDataStore?.putInt(key, value)
                    summary = textForValue(currentValue)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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

    private fun textForValue(value: Int) = "$value$unit"
}
