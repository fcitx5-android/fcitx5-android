/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.FcitxApplication
import timber.log.Timber

class RecentlyUsed(val type: String, val limit: Int) {

    companion object {
        // for backwords compatibility only
        const val DIR_NAME = "recently_used"
        const val PREFERENCE_NAME = "picker_recently_used"
    }

    private val sharedPreferences = FcitxApplication.getInstance().directBootAwareContext
        .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    private val map = LinkedHashMap<String, Boolean>(limit).apply {
        (migrate() ?: load()).forEach { put(it, true) }
    }

    val items: List<String> get() = map.keys.reversed()

    private fun load(): List<String> {
        val rawValue = sharedPreferences.getString(type, "") ?: ""
        if (rawValue.isEmpty()) {
            return emptyList()
        }
        return try {
            Json.decodeFromString<List<String>>(rawValue)
        } catch (_: Exception) {
            sharedPreferences.edit {
                remove(type)
            }
            emptyList()
        }
    }

    private fun save() {
        sharedPreferences.edit {
            putString(type, Json.encodeToString<List<String>>(map.keys.toList()))
        }
    }

    fun insert(item: String) {
        map.put(item, true)
        save()
    }

    fun migrate(): List<String>? {
        val dir = FcitxApplication.getInstance().directBootAwareContext.filesDir.resolve(DIR_NAME)
        val file = dir.resolve(type)
        if (file.exists()) {
            try {
                val lines = file.readLines()
                // save to sharedPreferences before deleting old file
                sharedPreferences.edit {
                    putString(type, Json.encodeToString<List<String>>(lines))
                }
                file.delete()
                if (dir.list()?.isEmpty() == true) {
                    dir.delete()
                }
                return lines
            } catch (e: Exception) {
                Timber.w("Failed to migrate RecentlyUsed(type=$type)")
                Timber.w(e)
                return null
            }
        }
        return null
    }
}