package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.preference.Preference
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TwinSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    var min: Int = 0
    var max: Int = 100
    var step: Int = 1
    var unit: String = ""

    var label: String = ""
    var secondaryKey: String = ""
    var secondaryLabel: String = ""

    var value = 0
        private set
    var secondaryValue = 0
        private set

    override fun onSetInitialValue(defaultValue: Any?) {
        preferenceDataStore?.apply {
            value = getInt(key, 0)
            secondaryValue = getInt(secondaryKey, 0)
        } ?: sharedPreferences?.apply {
            value = getInt(key, 0)
            secondaryValue = getInt(secondaryKey, 0)
        }
    }

    override fun setDefaultValue(defaultValue: Any?) {
        (defaultValue as? Pair<*, *>)?.apply {
            (first as? Int)?.let { value = it }
            (second as? Int)?.let { secondaryValue = it }
        }
    }

    private fun persistValues(primary: Int, secondary: Int) {
        if (!shouldPersist()) return
        value = primary
        secondaryValue = secondary
        preferenceDataStore?.apply {
            putInt(key, primary)
            putInt(secondaryKey, secondary)
        } ?: sharedPreferences?.edit {
            putInt(key, primary)
            putInt(secondaryKey, secondary)
        }
    }

    override fun onClick() {
        showDialog()
    }

    private fun SeekBar.setOnChangeListener(listener: SeekBar.(progress: Int) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                listener.invoke(seekBar, progress)
            }
        })
    }

    @OptIn(ExperimentalContracts::class)
    private fun ConstraintLayout.addSeekBar(
        label: String,
        initialValue: Int,
        belowView: View? = null,
        initSeekBar: SeekBar.() -> Unit = {}
    ) {
        contract { callsInPlace(initSeekBar, InvocationKind.EXACTLY_ONCE) }
        val textLabel = textView {
            text = label
        }
        val valueLabel = textView {
            text = textForValue(initialValue)
        }
        val seekBar = seekBar {
            max = progressForValue(this@TwinSeekBarPreference.max)
            progress = progressForValue(initialValue)
            setOnChangeListener { valueLabel.text = textForValue(valueForProgress(progress)) }
            initSeekBar(this)
        }
        val textMargin = dp(24)
        val seekBarMargin = dp(10)
        add(textLabel, lParams(wrapContent, wrapContent) {
            if (belowView == null) topOfParent(textMargin)
            else below(belowView, textMargin)
            startOfParent(textMargin)
        })
        add(valueLabel, lParams(wrapContent, wrapContent) {
            if (belowView == null) topOfParent(textMargin)
            else below(belowView, textMargin)
            endOfParent(textMargin)
        })
        add(seekBar, lParams(matchConstraints, wrapContent) {
            below(valueLabel, seekBarMargin)
            centerHorizontally(seekBarMargin)
        })
    }

    private fun showDialog() = with(context) {
        val primarySeekBar: SeekBar
        val secondarySeekBar: SeekBar
        val dialogContent = constraintLayout {
            addSeekBar(label, value) {
                primarySeekBar = this
            }
            addSeekBar(secondaryLabel, secondaryValue, primarySeekBar) {
                secondarySeekBar = this
            }
        }
        AlertDialog.Builder(context)
            .setTitle(this@TwinSeekBarPreference.title)
            .setView(dialogContent)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val primary = valueForProgress(primarySeekBar.progress)
                val secondary = valueForProgress(secondarySeekBar.progress)
                if (callChangeListener(primary to secondary)) {
                    persistValues(primary, secondary)
                    notifyChanged()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun progressForValue(value: Int) = (value - min) / step

    private fun valueForProgress(progress: Int) = (progress * step) + min

    private fun textForValue(value: Int) = "$value $unit"

    object SimpleSummaryProvider : SummaryProvider<TwinSeekBarPreference> {
        override fun provideSummary(preference: TwinSeekBarPreference): CharSequence {
            return preference.run { "${textForValue(value)} / ${textForValue(secondaryValue)}" }
        }
    }

}
