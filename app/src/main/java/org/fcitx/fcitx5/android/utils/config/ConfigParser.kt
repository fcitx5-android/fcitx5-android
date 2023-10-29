/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils.config

import arrow.core.Either

interface ConfigParser<I, O, E> {
    fun parse(raw: I): Either<E, O>
}