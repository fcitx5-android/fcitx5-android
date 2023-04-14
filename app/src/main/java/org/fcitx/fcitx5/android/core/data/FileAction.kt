package org.fcitx.fcitx5.android.core.data

interface FileAction {
    val path: String

    /**
     * We want to create files first, then update files, and finally delete directories and files.
     */
    val ordinal: Int

    /**
     * To create or update a file, we need its source.
     */
    interface Sourced {
        val src: FileSource
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