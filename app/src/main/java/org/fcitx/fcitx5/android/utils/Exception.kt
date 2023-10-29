/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import androidx.annotation.StringRes

inline fun <T : Throwable> errorT(
    cons: (String) -> T,
    @StringRes messageTemplate: Int,
    messageArg: String? = null
): Nothing =
    throw cons(
        messageArg?.let {
            appContext.getString(messageTemplate, it)
        } ?: appContext.getString(
            messageTemplate
        )
    )

fun errorState(@StringRes messageTemplate: Int, messageArg: String? = null): Nothing =
    errorT(::IllegalStateException, messageTemplate, messageArg)

fun errorArg(@StringRes messageTemplate: Int, messageArg: String? = null): Nothing =
    errorT(::IllegalArgumentException, messageTemplate, messageArg)

fun errorRuntime(@StringRes messageTemplate: Int, messageArg: String? = null): Nothing =
    errorT(::RuntimeException, messageTemplate, messageArg)