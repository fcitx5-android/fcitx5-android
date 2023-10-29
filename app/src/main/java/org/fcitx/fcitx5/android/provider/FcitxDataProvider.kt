/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.provider

import android.content.res.AssetFileDescriptor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import org.fcitx.fcitx5.android.R
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class FcitxDataProvider : DocumentsProvider() {

    companion object {
        private const val MIME_TYPE_WILDCARD = "*/*"
        private const val MIME_TYPE_TEXT = "text/plain"
        private const val MIME_TYPE_BIN = "application/octet-stream"

        private val TEXT_EXTENSIONS = arrayOf(
            "conf",
            "mb",
            "lua",
            "yml",
            "yaml"
        )

        // path relative to baseDir that should be recognize as text files
        private val TEXT_FILES = arrayOf(
            "config/config",
            "config/profile",
            "data/punctuation/punc.mb.zh_CN",
            "data/punctuation/punc.mb.zh_HK",
            "data/punctuation/punc.mb.zh_TW"
        )

        // The default columns to return information about a root if no specific
        // columns are requested in a query.
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_MIME_TYPES,
        )

        // The default columns to return information about a document if no specific
        // columns are requested in a query.
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        private const val SEARCH_RESULTS_LIMIT = 50
    }

    private lateinit var baseDir: File
    private lateinit var docIdPrefix: String
    private lateinit var textFilePaths: Array<String>

    private val File.docId
        get() = absolutePath.removePrefix(docIdPrefix)

    private fun fileFromDocId(docId: String) = File(docIdPrefix, docId)

    override fun onCreate(): Boolean {
        baseDir = context!!.getExternalFilesDir(null)!!
        docIdPrefix = "${baseDir.parent}${File.separator}"
        textFilePaths = Array(TEXT_FILES.size) { baseDir.resolve(TEXT_FILES[it]).absolutePath }
        return true
    }

    override fun queryRoots(projection: Array<String>?) =
        MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            newRow().apply {
                add(Root.COLUMN_ROOT_ID, baseDir.docId)
                add(
                    Root.COLUMN_FLAGS,
                    Root.FLAG_SUPPORTS_CREATE or Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD
                )
                add(Root.COLUMN_ICON, R.mipmap.app_icon)
                add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))
                add(Root.COLUMN_DOCUMENT_ID, baseDir.docId)
                add(Root.COLUMN_MIME_TYPES, MIME_TYPE_WILDCARD)
            }
        }

    override fun queryDocument(documentId: String, projection: Array<out String>?) =
        MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            newRowFromFile(fileFromDocId(documentId))
        }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ) = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
        fileFromDocId(parentDocumentId).listFiles()?.forEach {
            newRowFromFile(it)
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        return ParcelFileDescriptor.open(
            fileFromDocId(documentId),
            ParcelFileDescriptor.parseMode(mode)
        )
    }

    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        val file = fileFromDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val newFile = createAbstractFile(parentDocumentId, displayName)
        try {
            val ok = if (mimeType == Document.MIME_TYPE_DIR) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
            }
            if (!ok) {
                throw FileNotFoundException("createDocument id=${newFile.path} failed")
            }
        } catch (e: IOException) {
            throw FileNotFoundException("createDocument id=${newFile.path} failed: ${e.message}")
        }
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        fileFromDocId(documentId).apply {
            val ok = if (isDirectory) {
                deleteRecursively()
            } else {
                delete()
            }
            if (!ok) {
                throw FileNotFoundException("deleteDocument id=$documentId failed")
            }
        }
    }

    override fun getDocumentType(documentId: String): String {
        return fileFromDocId(documentId).mimeType
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    @Throws(FileNotFoundException::class)
    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String {
        val oldFile = fileFromDocId(sourceDocumentId)
        val newFile = createAbstractFile(targetParentDocumentId, oldFile.name)
        oldFile.apply {
            try {
                val ok = if (isDirectory) {
                    copyRecursively(newFile)
                } else {
                    copyTo(newFile).exists()
                }
                if (!ok) {
                    throw FileNotFoundException("copyDocument id=$sourceDocumentId to ${newFile.docId} failed")
                }
            } catch (e: Exception) {
                throw FileNotFoundException("copyDocument id=$sourceDocumentId to ${newFile.docId} failed: ${e.message}")
            }
        }
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {
        val oldFile = fileFromDocId(documentId)
        val newFile = oldFile.resolveSibling(displayName)
        if (newFile.exists()) {
            throw FileNotFoundException("renameDocument id=$documentId to $displayName failed: target exists")
        }
        oldFile.renameTo(newFile)
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun moveDocument(
        sourceDocumentId: String,
        sourceParentDocumentId: String,
        targetParentDocumentId: String
    ): String {
        val oldFile = fileFromDocId(sourceDocumentId)
        val newFile = createAbstractFile(targetParentDocumentId, oldFile.name)
        oldFile.renameTo(newFile)
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?
    ) = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
        val q = query.lowercase()
        fileFromDocId(rootId).walk()
            .filter { it.name.lowercase().contains(q) }
            .take(SEARCH_RESULTS_LIMIT)
            .forEach { newRowFromFile(it) }
    }

    private val File.mimeType: String
        get() = when {
            isDirectory -> Document.MIME_TYPE_DIR
            TEXT_EXTENSIONS.contains(extension) -> MIME_TYPE_TEXT
            textFilePaths.contains(absolutePath) -> MIME_TYPE_TEXT
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: MIME_TYPE_BIN
        }

    private fun createAbstractFile(parentDocumentId: String, displayName: String): File {
        val parent = fileFromDocId(parentDocumentId)
        var newFile = parent.resolve(displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = parent.resolve("$displayName ($noConflictId)")
            noConflictId += 1
        }
        return newFile
    }

    @Throws(FileNotFoundException::class)
    private fun MatrixCursor.newRowFromFile(file: File) {
        if (!file.exists()) {
            throw FileNotFoundException("File(path=${file.absolutePath}) not found")
        }

        val mimeType = file.mimeType
        var flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Document.FLAG_SUPPORTS_COPY else 0
        if (file.canWrite()) {
            flags = flags or if (file.isDirectory) {
                Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                Document.FLAG_SUPPORTS_WRITE
            }
        }
        if (file.parentFile?.canWrite() == true) {
            flags = flags or
                    Document.FLAG_SUPPORTS_DELETE or
                    Document.FLAG_SUPPORTS_RENAME
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = flags or Document.FLAG_SUPPORTS_MOVE
            }
        }
        if (mimeType.startsWith("image/")) {
            flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
        }

        newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, file.docId)
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_SIZE, file.length())
        }
    }
}
