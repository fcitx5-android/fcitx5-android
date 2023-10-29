/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data

import org.fcitx.fcitx5.android.FcitxApplication

// Not thread-safe
class RecentlyUsed(
    val fileName: String,
    val capacity: Int
) : LinkedHashMap<String, String>(0, .75f, true) {

    companion object {
        const val DIR_NAME = "recently_used"
    }

    private val file =
        FcitxApplication.getInstance().directBootAwareContext.filesDir.resolve(DIR_NAME).run {
            mkdirs()
            resolve(fileName).apply { createNewFile() }
        }

    fun load() {
        val xs = file.readLines()
        xs.forEach {
            if (it.isNotBlank())
                put(it, it)
        }
    }

    fun save() {
        file.writeText(values.joinToString("\n"))
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) =
        size > capacity

    fun insert(s: String) = put(s, s)

    fun toOrderedList() = values.toList().reversed()
}