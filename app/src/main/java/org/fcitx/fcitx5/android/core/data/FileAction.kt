/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core.data

sealed interface FileAction {
    val path: String

    /**
     * We want to create files first, then update files, delete directories and files, and finally create symlinks
     */
    val ordinal: Int

    /**
     * To create or update a file, we need its source.
     */
    interface Sourced {
        val src: FileSource
    }

    data class CreateSymlink(override val path: String, val src: String) : FileAction {
        override val ordinal: Int
            get() = -1
    }

    data class CreateFile(override val path: String, override val src: FileSource) :
        FileAction,
        Sourced {
        override val ordinal: Int
            get() = 3
    }

    data class UpdateFile(override val path: String, override val src: FileSource) :
        FileAction,
        Sourced {
        override val ordinal: Int
            get() = 2
    }

    data class DeleteFile(override val path: String) : FileAction {
        override val ordinal: Int
            get() = 0
    }

    data class DeleteDir(override val path: String) : FileAction {
        override val ordinal: Int
            get() = 1
    }
}