package org.fcitx.fcitx5.android.data

import org.fcitx.fcitx5.android.utils.appContext

// Not thread-safe
class RecentlyUsed(
    val name: String,
    val capacity: Int
) : LinkedHashMap<String, String>(0, .75f, true) {

    private val file = appContext.filesDir.resolve("recently_used").run {
        mkdirs()
        resolve(name).apply { createNewFile() }
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