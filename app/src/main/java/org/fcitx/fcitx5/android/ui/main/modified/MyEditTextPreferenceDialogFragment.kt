/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.modified

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import org.fcitx.fcitx5.android.R

class MyEditTextPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {
    private lateinit var editText: EditText

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val p = preference as EditTextPreference
        builder.setNeutralButton(R.string.default_) { _, _ ->
            p.restore()
        }
        super.onPrepareDialogBuilder(builder)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        fixDialogMargin(view)
        editText = view.findViewById(android.R.id.edit)
        editText.addTextChangedListener { editText.error = null }
    }

    override fun onStart() {
        super.onStart()
        val p = preference as? ValidatedEditTextPreference ?: return
        if (p.validator == null) return
        val alertDialog = dialog as? AlertDialog ?: return
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (p.isValid(editText.text.toString())) {
                onClick(alertDialog, DialogInterface.BUTTON_POSITIVE)
                alertDialog.dismiss()
            } else {
                editText.error = getString(p.validationError)
            }
        }
    }

    companion object {
        fun newInstance(key: String): MyEditTextPreferenceDialogFragment {
            val fragment = MyEditTextPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}