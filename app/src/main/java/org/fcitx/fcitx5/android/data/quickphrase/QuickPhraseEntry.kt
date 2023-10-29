/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

data class QuickPhraseEntry(val keyword: String, val phrase: String) {
    fun serialize() = "$keyword ${phrase.replace("\n", "\\n")}"
}