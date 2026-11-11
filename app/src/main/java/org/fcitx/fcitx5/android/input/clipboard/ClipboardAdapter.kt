/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Patterns
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.utils.DeviceUtil
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.queryFileName
import splitties.resources.styledColor
import timber.log.Timber
import kotlin.math.min
import android.provider.DocumentsContract

abstract class ClipboardAdapter(
    private val theme: Theme,
    private val entryRadius: Float,
    private val maskSensitive: Boolean
) : PagingDataAdapter<ClipboardEntry, ClipboardAdapter.ViewHolder>(diffCallback) {

    companion object {
        private val thumbnailCache = object : LruCache<String, Bitmap>(24) {}
        private val cnMainlandMobilePattern = Regex("^1[3-9]\\d{9}$")

        private val diffCallback = object : DiffUtil.ItemCallback<ClipboardEntry>() {
            override fun areItemsTheSame(
                oldItem: ClipboardEntry,
                newItem: ClipboardEntry
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ClipboardEntry,
                newItem: ClipboardEntry
            ): Boolean {
                return oldItem == newItem
            }
        }

        /**
         * excerpt text to show on ClipboardEntryUi, to reduce render time of very long text
         * @param str text to excerpt
         * @param mask mask text content with "•"
         * @param lines max output lines
         * @param chars max chars per output line
         */
        fun excerptText(
            str: String,
            mask: Boolean = false,
            lines: Int = 4,
            chars: Int = 128
        ): String = buildString {
            val length = str.length
            var lineBreak = -1
            for (i in 1..lines) {
                val start = lineBreak + 1   // skip previous '\n'
                val excerptEnd = min(start + chars, length)
                lineBreak = str.indexOf('\n', start)
                if (lineBreak < 0) {
                    // no line breaks remaining, substring to end of text
                    if (mask) {
                        append(ClipboardEntry.BULLET.repeat(excerptEnd - start))
                    } else {
                        append(str.substring(start, excerptEnd))
                    }
                    break
                } else {
                    val end = min(excerptEnd, lineBreak)
                    // append one line exactly
                    if (mask) {
                        append(ClipboardEntry.BULLET.repeat(end - start))
                    } else {
                        appendLine(str.substring(start, end))
                    }
                }
            }
        }
    }

    private var popupMenu: PopupMenu? = null

    class ViewHolder(val entryUi: ClipboardEntryUi) : RecyclerView.ViewHolder(entryUi.root) {
        var thumbnailJob: Job? = null
        var boundThumbnailKey: String? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ClipboardEntryUi(parent.context, theme, entryRadius))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position) ?: return
        with(holder.entryUi) {
            val displayText = if (entry.isUriEntry()) {
                compactUriLabel(ctx, entry)
            } else {
                excerptText(entry.text, entry.sensitive && maskSensitive)
            }
            val linkUri = entry.openableLinkUri()
            val searchQuery = entry.searchableQuery()
            val dialNumber = entry.dialableCnMobileNumber()
            val splittableText = entry.splittableText()
            val thumbnailKey = entry.imagePreviewKey()
            val cachedThumbnail = thumbnailKey?.let { thumbnailCache.get(it) }
            holder.thumbnailJob?.cancel()
            holder.boundThumbnailKey = thumbnailKey
            setEntry(displayText, entry.pinned, cachedThumbnail)
            if (thumbnailKey != null && cachedThumbnail == null) {
                holder.thumbnailJob = scope.launch {
                    val bitmap = loadImagePreview(ctx, entry)
                    if (bitmap != null) {
                        thumbnailCache.put(thumbnailKey, bitmap)
                    }
                    if (holder.boundThumbnailKey == thumbnailKey) {
                        holder.entryUi.setEntry(displayText, entry.pinned, bitmap)
                    }
                }
            }
            root.setOnClickListener {
                showEntryMenu(
                    anchor = root,
                    entry = entry,
                    linkUri = linkUri,
                    searchQuery = searchQuery,
                    dialNumber = dialNumber,
                    splittableText = splittableText
                )
            }
            root.setOnLongClickListener {
                showEntryMenu(
                    anchor = root,
                    entry = entry,
                    linkUri = linkUri,
                    searchQuery = searchQuery,
                    dialNumber = dialNumber,
                    splittableText = splittableText
                )
                true
            }
        }
    }

    private fun showEntryMenu(
        anchor: android.view.View,
        entry: ClipboardEntry,
        linkUri: Uri?,
        searchQuery: String?,
        dialNumber: String?,
        splittableText: String?
    ) {
        val popup = PopupMenu(anchor.context, anchor)
        val menu = popup.menu
        val iconTint = anchor.context.styledColor(android.R.attr.colorControlNormal)
        val isUriEntry = entry.isUriEntry()
        val imageUri = entry.viewableImageUri()

        if (!isUriEntry) {
            menu.item(android.R.string.paste, iconTint = iconTint) {
                onPaste(entry)
            }
        }
        if (entry.pinned) {
            menu.item(R.string.unpin, R.drawable.ic_outline_push_pin_24, iconTint) {
                onUnpin(entry.id)
            }
        } else {
            menu.item(R.string.pin, R.drawable.ic_baseline_push_pin_24, iconTint) {
                onPin(entry.id)
            }
        }
        if (!isUriEntry) {
            menu.item(R.string.edit, R.drawable.ic_baseline_edit_24, iconTint) {
                onEdit(entry.id)
            }
        } else if (imageUri == null) {
            menu.item(R.string.open_link, R.drawable.ic_baseline_language_24, iconTint) {
                onOpenFile(entry)
            }
        }
        menu.item(R.string.share, R.drawable.ic_baseline_share_24, iconTint) {
            onShare(entry)
        }
        if (shouldShowUpload(entry)) {
            menu.item(R.string.upload, R.drawable.ic_baseline_send_24, iconTint) {
                onUpload(entry)
            }
        }
        if (splittableText != null) {
            menu.item(R.string.split_words, R.drawable.ic_baseline_spellcheck_24, iconTint) {
                onSplitText(splittableText)
            }
        }
        imageUri?.let { uri ->
            menu.item(R.string.view_image, R.drawable.ic_baseline_image_24, iconTint) {
                onViewImage(uri)
            }
        }
        if (linkUri != null) {
            menu.item(R.string.open_link, R.drawable.ic_baseline_language_24, iconTint) {
                onOpenLink(linkUri)
            }
        }
        if (searchQuery != null) {
            menu.item(R.string.search, R.drawable.ic_baseline_search_24, iconTint) {
                onSearch(searchQuery)
            }
        }
        if (dialNumber != null) {
            menu.item(R.string.dial, R.drawable.ic_baseline_call_24, iconTint) {
                onDial(dialNumber)
            }
        }
        menu.item(R.string.delete, R.drawable.ic_baseline_delete_24, iconTint) {
            onDelete(entry.id)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !DeviceUtil.isSamsungOneUI && !DeviceUtil.isFlyme) {
            popup.setForceShowIcon(true)
        }
        popup.setOnDismissListener {
            if (it === popupMenu) popupMenu = null
        }
        popupMenu?.dismiss()
        popupMenu = popup
        popup.show()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.thumbnailJob?.cancel()
        holder.thumbnailJob = null
        holder.boundThumbnailKey = null
        super.onViewRecycled(holder)
    }

    fun getEntryAt(position: Int) = getItem(position)

    fun onDetached() {
        scope.cancel()
        popupMenu?.dismiss()
        popupMenu = null
    }

    abstract fun onPaste(entry: ClipboardEntry)

    abstract fun onPin(id: Int)

    abstract fun onUnpin(id: Int)

    abstract fun onEdit(id: Int)

    abstract fun onOpenFile(entry: ClipboardEntry)

    abstract fun onShare(entry: ClipboardEntry)

    open fun shouldShowUpload(entry: ClipboardEntry): Boolean = false

    open fun onUpload(entry: ClipboardEntry) = Unit

    abstract fun onSplitText(text: String)

    abstract fun onSearch(query: String)

    abstract fun onDial(number: String)

    abstract fun onOpenLink(uri: Uri)

    abstract fun onViewImage(uri: Uri)

    abstract fun onDelete(id: Int)

    private fun compactUriLabel(context: Context, entry: ClipboardEntry): String {
        val uri = runCatching { Uri.parse(entry.text) }.getOrNull()
        val fileName = uri?.let { resolveUriFileName(context, it) }
        return if (entry.type.startsWith("image/")) {
            ""
        } else {
            if (fileName.isNullOrBlank()) {
                context.getString(R.string.clipboard_entry_file)
            } else {
                context.getString(R.string.clipboard_entry_file_named, fileName)
            }
        }
    }

    private fun resolveUriFileName(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> context.contentResolver.queryFileName(uri)
                ?: uri.lastPathSegment
                ?: uri.path

            "file" -> uri.lastPathSegment ?: uri.path
            else -> uri.lastPathSegment ?: uri.path
        }?.let { Uri.decode(it).substringAfterLast('/').substringAfterLast(':') }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun ClipboardEntry.openableLinkUri(): Uri? {
        if (isUriEntry()) return null
        val raw = text.trim()
        if (raw.isEmpty()) return null
        val direct = runCatching { raw.toUri() }.getOrNull()
        if (direct != null &&
            direct.scheme?.lowercase() in setOf("http", "https") &&
            !direct.host.isNullOrBlank()
        ) {
            return direct
        }
        return if (!raw.contains('\n') && Patterns.WEB_URL.matcher(raw).matches()) {
            "https://$raw".toUri()
        } else {
            null
        }
    }

    private fun ClipboardEntry.searchableQuery(): String? {
        if (isUriEntry()) return null
        val raw = text.trim()
        return raw.takeIf { it.isNotEmpty() }
    }

    private fun ClipboardEntry.splittableText(): String? {
        if (isUriEntry()) return null
        val raw = text.trim()
        return raw.takeIf { it.length > 10 }
    }

    private fun ClipboardEntry.dialableCnMobileNumber(): String? {
        if (isUriEntry()) return null
        val raw = text.trim()
        if (raw.isEmpty() || raw.contains('\n')) return null
        val compact = raw
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("（", "")
            .replace("）", "")
        val normalized = when {
            compact.startsWith("+86") -> compact.removePrefix("+86")
            compact.startsWith("86") && compact.length > 11 -> compact.removePrefix("86")
            else -> compact
        }
        if (!normalized.all { it.isDigit() }) return null
        return normalized.takeIf { cnMainlandMobilePattern.matches(it) }
    }

    private fun ClipboardEntry.imagePreviewKey(): String? {
        return if (isUriEntry() && type.startsWith("image/")) text else null
    }

    private fun ClipboardEntry.viewableImageUri(): Uri? {
        if (!isUriEntry() || !type.startsWith("image/")) return null
        return runCatching { Uri.parse(text) }.getOrNull()
    }

    private suspend fun loadImagePreview(context: Context, entry: ClipboardEntry): Bitmap? {
        if (!entry.isUriEntry() || !entry.type.startsWith("image/")) return null
        val originalUri = runCatching { Uri.parse(entry.text) }.getOrNull() ?: run {
            Timber.w("loadImagePreview: failed to parse URI from entry.text")
            return null
        }
        Timber.d("loadImagePreview: attempting to load from URI: $originalUri")

        // For ExternalStorageProvider tree URIs, try to convert to file path and load directly
        var uri = originalUri
        if (isExternalStorageProviderTreeUri(originalUri)) {
            val filePath = extractFilePathFromTreeUri(originalUri)
            if (filePath != null) {
                uri = Uri.parse("file://$filePath")
                Timber.d("loadImagePreview: converted tree URI to file path: $uri")
            }
        }

        return withContext(Dispatchers.IO) {
            // Retry up to 3 times with delay between attempts
            repeat(3) { attempt ->
                val bitmap = runCatching {
                    val resolver = context.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        Timber.w("loadImagePreview: bounds invalid (${bounds.outWidth}x${bounds.outHeight})")
                        return@runCatching null
                    }
                    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 192, 192)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    resolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }.getOrNull()
                if (bitmap != null) {
                    Timber.d("loadImagePreview: successfully loaded bitmap ${bitmap.width}x${bitmap.height}")
                    return@withContext bitmap
                }
                Timber.w("loadImagePreview: attempt ${attempt + 1} failed, retrying...")
                // Wait a bit before retry (except on last attempt)
                if (attempt < 2) {
                    kotlinx.coroutines.delay(100L shl attempt) // 100ms, 200ms
                }
            }
            // All attempts failed
            Timber.e("loadImagePreview: all 3 attempts failed for URI: $uri")
            null
        }
    }

    private fun isExternalStorageProviderTreeUri(uri: Uri): Boolean {
        return uri.authority == "com.android.externalstorage.documents" &&
                uri.path?.startsWith("/tree/") == true
    }

    private fun extractFilePathFromTreeUri(treeUri: Uri): String? {
        val documentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: Exception) {
            Timber.w("Failed to get document ID from tree URI: $treeUri")
            return null
        }
        val parts = documentId.split(":", limit = 2)
        if (parts.size != 2) return null
        val (volume, relativePath) = parts[0] to parts[1]
        return when {
            volume.equals("primary", ignoreCase = true) -> {
                if (relativePath.isBlank()) {
                    "/storage/emulated/0"
                } else {
                    "/storage/emulated/0/$relativePath"
                }
            }
            else -> "/storage/$volume/$relativePath"
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentHeight / 2 >= reqHeight && currentWidth / 2 >= reqWidth) {
            currentHeight /= 2
            currentWidth /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

}
