package org.fcitx.fcitx5.android.clipboardsync

import android.content.Context
import android.content.SharedPreferences
import org.fcitx.fcitx5.android.R
import java.util.Locale

object SyncFilterPrefs {
    const val PREF_FILTER_ENTRY = "sync_filter"
    const val PREF_FILTER_BLOCKED_EXTENSIONS = "filter_blocked_extensions"
    const val PREF_FILTER_MAX_FILE_SIZE = "filter_max_file_size"
    const val PREF_FILTER_MAX_FILE_SIZE_UNIT = "filter_max_file_size_unit"
    const val PREF_FILTER_MIN_TEXT_CHARS = "filter_min_text_chars"
    const val PREF_FILTER_MAX_TEXT_CHARS = "filter_max_text_chars"
    const val PREF_FILTER_PREVIEW = "sync_filter_preview"

    enum class FileSizeUnit(val prefValue: String, val bytes: Long) {
        KB("KB", 1024L),
        MB("MB", 1024L * 1024L),
        GB("GB", 1024L * 1024L * 1024L);

        companion object {
            fun fromPrefValue(value: String?): FileSizeUnit {
                return entries.firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: MB
            }
        }
    }

    data class State(
        val blockedExtensions: Set<String>,
        val maxFileSizeValue: Long?,
        val maxFileSizeUnit: FileSizeUnit,
        val minTextChars: Int?,
        val maxTextChars: Int?
    ) {
        val hasActiveRule: Boolean
            get() = blockedExtensions.isNotEmpty() ||
                maxFileSizeValue != null ||
                minTextChars != null ||
                maxTextChars != null

        val maxFileSizeBytes: Long?
            get() = maxFileSizeValue?.times(maxFileSizeUnit.bytes)
    }

    fun ensureDefaults(prefs: SharedPreferences) {
        if (!prefs.contains(PREF_FILTER_MAX_FILE_SIZE_UNIT)) {
            prefs.edit().putString(PREF_FILTER_MAX_FILE_SIZE_UNIT, FileSizeUnit.MB.prefValue).apply()
        }
    }

    fun loadState(prefs: SharedPreferences): State {
        ensureDefaults(prefs)
        val blockedExtensions = prefs.getString(PREF_FILTER_BLOCKED_EXTENSIONS, null)
            .orEmpty()
            .split(Regex("[,\\s]+"))
            .map { it.trim().removePrefix(".").lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .toSet()

        val normalizedTextBounds = normalizeBounds(
            parsePositiveInt(prefs, PREF_FILTER_MIN_TEXT_CHARS),
            parsePositiveInt(prefs, PREF_FILTER_MAX_TEXT_CHARS)
        )

        return State(
            blockedExtensions = blockedExtensions,
            maxFileSizeValue = parsePositiveLong(prefs, PREF_FILTER_MAX_FILE_SIZE),
            maxFileSizeUnit = FileSizeUnit.fromPrefValue(
                prefs.getString(PREF_FILTER_MAX_FILE_SIZE_UNIT, FileSizeUnit.MB.prefValue)
            ),
            minTextChars = normalizedTextBounds.first,
            maxTextChars = normalizedTextBounds.second
        )
    }

    fun buildSummary(context: Context, prefs: SharedPreferences): String {
        val state = loadState(prefs)
        if (!state.hasActiveRule) {
            return context.getString(R.string.sync_filter_summary_empty)
        }
        return buildRuleSegments(context, state).joinToString("，")
    }

    fun buildPreview(context: Context, prefs: SharedPreferences): String {
        val state = loadState(prefs)
        if (!state.hasActiveRule) {
            return context.getString(R.string.sync_filter_preview_empty)
        }
        return buildRuleSegments(context, state).joinToString(separator = "\n")
    }

    private fun buildRuleSegments(context: Context, state: State): List<String> {
        val segments = mutableListOf<String>()

        if (state.minTextChars != null || state.maxTextChars != null) {
            segments += when {
                state.minTextChars != null && state.maxTextChars != null ->
                    context.getString(
                        R.string.sync_filter_segment_text_range,
                        state.minTextChars,
                        state.maxTextChars
                    )
                state.minTextChars != null ->
                    context.getString(R.string.sync_filter_segment_text_min, state.minTextChars)
                else ->
                    context.getString(R.string.sync_filter_segment_text_max, state.maxTextChars ?: 0)
            }
        }

        if (state.blockedExtensions.isNotEmpty()) {
            val value = state.blockedExtensions.joinToString("/") { ".$it" }
            segments += context.getString(R.string.sync_filter_segment_extensions, value)
        }

        state.maxFileSizeValue?.let { sizeValue ->
            segments += context.getString(
                R.string.sync_filter_segment_file_max,
                sizeValue,
                state.maxFileSizeUnit.prefValue
            )
        }

        return segments
    }

    private fun parsePositiveLong(prefs: SharedPreferences, key: String): Long? {
        return prefs.getString(key, null)
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
    }

    private fun parsePositiveInt(prefs: SharedPreferences, key: String): Int? {
        return prefs.getString(key, null)
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    }

    private fun normalizeBounds(minValue: Int?, maxValue: Int?): Pair<Int?, Int?> {
        return when {
            minValue == null -> null to maxValue
            maxValue == null -> minValue to null
            minValue <= maxValue -> minValue to maxValue
            else -> maxValue to minValue
        }
    }
}
