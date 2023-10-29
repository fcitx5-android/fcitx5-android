/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Key

class FcitxKeyPreference : Preference {

    private var keyString = ""

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, androidx.preference.R.attr.preferenceStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.DialogSeekBarPreference, 0, 0).run {
            try {
                if (getBoolean(R.styleable.FcitxKeyPreference_useSimpleSummaryProvider, false)) {
                    summaryProvider = SimpleSummaryProvider
                }
            } finally {
                recycle()
            }
        }
    }

    private val currentValue: String
        get() = getPersistedString(keyString)

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        keyString = defaultValue as? String ?: ""
    }

    override fun onClick() {
        showDialog()
    }

    private fun showDialog() {
        val ui = KeyPreferenceUi(context).apply { setKey(Key.parse(currentValue)) }
        AlertDialog.Builder(context)
            .setTitle(this@FcitxKeyPreference.title)
            .setView(ui.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = ui.lastKey.portableString
                if (callChangeListener(value)) {
                    persistString(value)
                    notifyChanged()
                }
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    object SimpleSummaryProvider : SummaryProvider<FcitxKeyPreference> {
        override fun provideSummary(preference: FcitxKeyPreference): CharSequence {
            return Key.parse(preference.currentValue).localizedString.ifEmpty {
                preference.context.getString(R.string.none)
            }
        }
    }
}
