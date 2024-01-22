/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.app.AlertDialog
import android.widget.Button

val AlertDialog.positiveButton: Button
    get() = getButton(AlertDialog.BUTTON_POSITIVE)

val AlertDialog.negativeButton: Button
    get() = getButton(AlertDialog.BUTTON_NEGATIVE)

val AlertDialog.neutralButton: Button
    get() = getButton(AlertDialog.BUTTON_NEUTRAL)

/**
 * Change positive button listener **AFTER** [AlertDialog.show] has been called.
 *
 * In the listener: `true` to dismiss the dialog; `false` to keep the dialog open.
 */
fun AlertDialog.onPositiveButtonClick(l: AlertDialog.() -> Boolean?): AlertDialog {
    positiveButton.setOnClickListener {
        if (l.invoke(this) == true) dismiss()
    }
    return this
}

/**
 * Change negative button listener **AFTER** [AlertDialog.show] has been called.
 *
 * In the listener: `true` to dismiss the dialog; `false` to keep the dialog open.
 */
fun AlertDialog.onNegativeButtonClick(l: AlertDialog.() -> Boolean): AlertDialog {
    negativeButton.setOnClickListener {
        if (l.invoke(this)) dismiss()
    }
    return this
}

/**
 * Change neutral button listener **AFTER** [AlertDialog.show] has been called.
 *
 * In the listener: `true` to dismiss the dialog; `false` to keep the dialog open.
 */
fun AlertDialog.onNeutralButtonClick(l: AlertDialog.() -> Boolean): AlertDialog {
    neutralButton.setOnClickListener {
        if (l.invoke(this)) dismiss()
    }
    return this
}
