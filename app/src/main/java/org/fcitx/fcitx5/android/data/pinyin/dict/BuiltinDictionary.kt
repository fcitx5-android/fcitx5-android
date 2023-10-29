/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.pinyin.dict

import java.io.File

class BuiltinDictionary(override val file: File) : PinyinDictionary() {
    override val type: Type
        get() = Type.LibIME

    private val delegate by lazy { LibIMEDictionary(file) }

    override fun toTextDictionary(dest: File): TextDictionary = delegate.toTextDictionary(dest)

    override fun toLibIMEDictionary(dest: File): LibIMEDictionary = delegate

}