/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.quickphrase

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import java.io.File

@Serializable(QuickPhraseSerializer::class)
abstract class QuickPhrase : Parcelable {

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(file.absolutePath)
        dest.writeByte(if (this is BuiltinQuickPhrase) 1 else 0)
        if (this is BuiltinQuickPhrase) {
            dest.writeString(overrideFilePath)
        } else {
            dest.writeString(null)
        }
    }

    abstract val file: File

    abstract val isEnabled: Boolean

    open val name: String
        get() = file.nameWithoutExtension

    protected fun ensureFileExists() {
        if (!file.exists())
            throw IllegalStateException("File ${file.absolutePath} does not exist")
    }

    abstract fun loadData(): QuickPhraseData

    abstract fun saveData(data: QuickPhraseData)

    abstract fun enable()

    abstract fun disable()

    companion object {
        const val EXT = "mb"
        const val DISABLE = "disable"
    }
}