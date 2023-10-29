/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core.data

import android.util.Base64
import java.security.MessageDigest

/**
 * Merge [DataDescriptor]s
 *
 * It records files' sources, i.e. what [DataDescriptor] they belong to
 */
class DataHierarchy {
    private val files = mutableMapOf<String, Pair<SHA256, FileSource>>()
    private val descriptorSHA256 = mutableSetOf<SHA256>()
    private val symlinks = mutableMapOf<String, Pair<String, FileSource>>()

    data class PathConflict(val path: String, val src: FileSource) : Exception()
    data class SymlinkConflict(val path: String, val src: FileSource) : Exception()

    /**
     * Merge a [DataDescriptor]
     *
     * @throws PathConflict if a non-directory path already exists in the hierarchy
     * @throws SymlinkConflict if a file or directory already exists when creating symlink
     */
    fun install(descriptor: DataDescriptor, src: FileSource) {
        val newFiles = descriptor.files.mapValues { (path, sha256) ->
            files[path]?.also { old ->
                // path conflict when at least one of them is not a directory (empty sha256)
                if (old.first.isNotEmpty() || sha256.isNotEmpty()) {
                    throw PathConflict(path, old.second)
                }
            }
            Pair(sha256, src)
        }
        // merge new files only when there is no conflict with existing files
        files.putAll(newFiles)
        val newSymlinks = descriptor.symlinks.mapValues { (path, source) ->
            // path we try to create is already a file or directory in our hierarchy
            files[path]?.let { (_, src) ->
                throw SymlinkConflict(path, src)
            }
            // path we try to create is already a symlink in our hierarchy
            // but it refers to a different path
            symlinks[path]?.let { (existedSource, src) ->
                if (source != existedSource)
                    throw PathConflict(path, src)
            }
            Pair(source, src)
        }
        symlinks.putAll(newSymlinks)
        descriptorSHA256.add(descriptor.sha256)
    }

    /**
     * Create a [DataDescriptor] from the file list, discarding other information
     */
    fun downToDataDescriptor() =
        DataDescriptor(
            sha256(this),
            files.mapValues { it.value.first },
            symlinks.mapValues { it.value.first })

    companion object {
        private val digest by lazy { MessageDigest.getInstance("SHA-256") }

        /**
         * Calculate checksum according to merged descriptors
         *
         * Note: This is different from sha256 calculated by gradle task,
         * in which the it is the hash string of file list itself
         */
        private fun sha256(h: DataHierarchy): String =
            digest.digest(h.descriptorSHA256.joinToString(separator = "").encodeToByteArray())
                .let {
                    Base64.encodeToString(it, 0).trim()
                }

        /**
         * Compute the difference between a [DataDescriptor] and [DataHierarchy],
         * generating [FileAction]s to migrate from the [old] to [new]
         */
        fun diff(old: DataDescriptor, new: DataHierarchy): List<FileAction> {
            if (old.sha256 == sha256(new))
                return emptyList()
            val diffFiles = new.files.mapNotNull { (path, v) ->
                val (sha256, src) = v
                when {
                    path !in old.files && sha256.isNotBlank() ->
                        FileAction.CreateFile(path, src)
                    old.files[path] != sha256 ->
                        if (sha256.isNotBlank())
                            FileAction.UpdateFile(path, src)
                        else null
                    else -> null
                }
            }.toMutableList<FileAction>().apply {
                addAll(old.files.filterKeys { it !in new.files }
                    .map { (path, sha256) ->
                        if (sha256.isNotBlank())
                            FileAction.DeleteFile(path)
                        else
                            FileAction.DeleteDir(path)
                    })
            }
            val diffLinks = new.symlinks.mapNotNull { (target, v) ->
                val (source, _) = v
                if (old.symlinks[target] == source)
                // old link will be overwritten
                    null
                else
                    FileAction.CreateSymlink(target, source)
            }.toMutableList<FileAction>().apply {
                addAll(old.symlinks.filterKeys { it !in new.symlinks }.map { (target, _) ->
                    FileAction.DeleteFile(target)
                })
            }
            return diffFiles + diffLinks
        }
    }
}