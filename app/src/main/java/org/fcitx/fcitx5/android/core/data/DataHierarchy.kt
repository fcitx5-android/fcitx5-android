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
    private val plugins = mutableSetOf<PluginDescriptor>()

    class Conflict(val path: String, val src: FileSource) : Exception()

    /**
     * Merge a [DataDescriptor]
     *
     * @throws Conflict if a non-directory path is existing in the file list
     */
    fun install(descriptor: DataDescriptor, src: FileSource) {
        if (descriptor.sha256 in descriptorSHA256)
            return
        descriptor.files.forEach { (path, sha256) ->
            if (path in files) {
                val old = files.getValue(path)
                if (old.first.isNotBlank() || sha256.isNotBlank())
                    throw Conflict(path, files[path]!!.second)
            }
            if (src is FileSource.Plugin)
                plugins += src.descriptor
            files[path] = sha256 to src
        }
        descriptorSHA256.add(descriptor.sha256)
    }

    /**
     * Create a [DataDescriptor] from the file list, discarding other information
     */
    fun downToDataDescriptor() =
        DataDescriptor(sha256(this), files.mapValues { it.value.first })

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
                    Base64.encodeToString(it, 0)
                }

        /**
         * Compute the difference between a [DataDescriptor] and [DataHierarchy],
         * generating [FileAction]s to migrate from the [old] to [new]
         */
        fun diff(old: DataDescriptor, new: DataHierarchy): List<FileAction> {
            if (old.sha256 == sha256(new))
                return emptyList()
            return new.files.mapNotNull { (path, v) ->
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
        }
    }
}