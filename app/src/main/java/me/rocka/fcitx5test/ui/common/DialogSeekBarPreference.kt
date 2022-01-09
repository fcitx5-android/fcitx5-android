package me.rocka.fcitx5test.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.DialogSeekBarBinding

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

    @Suppress("unused")
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, R.attr.preferenceStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            summary = getTextForValue(newValue.toString())
            true
        }
        onPreferenceClickListener = OnPreferenceClickListener {
            showSeekBarDialog()
            true
        }
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager?) {
        super.onAttachedToHierarchy(preferenceManager)
        summary = getTextForValue(
            preferenceDataStore?.getInt(key, defaultValue) ?: defaultValue
        )
    }

    /**
     * Generates the text for the given [value] and adds the defined [unit] at the end.
     */
    private fun getTextForValue(value: Any): String {
        return if (value !is Int) {
            "??$unit"
        } else {
            value.toString() + unit
        }
    }

    /**
     * Shows the seek bar dialog.
     */
    private fun showSeekBarDialog() {
        val dialogView = DialogSeekBarBinding.inflate(LayoutInflater.from(context))
        val initValue = preferenceDataStore?.getInt(key, defaultValue) ?: defaultValue
        dialogView.seekBar.max = actualValueToSeekBarProgress(max)
        dialogView.seekBar.progress = actualValueToSeekBarProgress(initValue)
        dialogView.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                dialogView.seekBarValue.text = getTextForValue(seekBarProgressToActualValue(progress))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        dialogView.seekBarValue.text = getTextForValue(initValue)
        AlertDialog.Builder(context).apply {
            setTitle(this@DialogSeekBarPreference.title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val actualValue = seekBarProgressToActualValue(dialogView.seekBar.progress)
                preferenceDataStore?.putInt(key, actualValue)
            }
            setNegativeButton(android.R.string.cancel, null)
            setOnDismissListener { summary = getTextForValue(
                preferenceDataStore?.getInt(key, defaultValue) ?: defaultValue
            ) }
            create()
            show()
        }
    }

    /**
     * Converts the actual value to a progress value which the Android SeekBar implementation can
     * handle. (Android's SeekBar step is fixed at 1 and min at 0)
     *
     * @param actual The actual value.
     * @return the internal value which is used to allow different min and step values.
     */
    private fun actualValueToSeekBarProgress(actual: Int): Int {
        return (actual - min) / step
    }

    /**
     * Converts the Android SeekBar value to the actual value.
     *
     * @param progress The progress value of the SeekBar.
     * @return the actual value which is ready to use.
     */
    private fun seekBarProgressToActualValue(progress: Int): Int {
        return (progress * step) + min
    }
}
