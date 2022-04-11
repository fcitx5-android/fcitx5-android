package org.fcitx.fcitx5.android.utils

import kotlin.properties.Delegates

class NaiveDustman<T> {

    private val initialValues: MutableMap<String, T> = mutableMapOf()
    private val dirtyStatus: MutableMap<String, Boolean> = mutableMapOf()
    private val newKeys = mutableListOf<String>()

    var dirty by Delegates.observable(false) { _, old, new ->
        if (old != new) {
            if (new)
                onDirty?.invoke()
            else
                onClean?.invoke()
        }
    }
        private set

    var onDirty: (() -> Unit)? = null
    var onClean: (() -> Unit)? = null

    fun forceDirty() {
        dirty = true
    }

    private fun updateDirtyStatus(key: String, boolean: Boolean) {
        dirtyStatus[key] = boolean
        dirty = newKeys.isNotEmpty() || dirtyStatus.any { it.value }
    }

    fun addOrUpdate(key: String, value: T) {
        when {
            (key !in initialValues) -> {
                initialValues[key] = value
                newKeys.add(key)
                updateDirtyStatus(key, false)
            }
            initialValues[key] == value -> {
                updateDirtyStatus(key, false)
            }
            else -> {
                updateDirtyStatus(key, true)
            }
        }
    }

    fun remove(key: String) {
        val a = initialValues.remove(key) != null
        val b = newKeys.remove(key)
        updateDirtyStatus(key, a || !b)
    }


    fun reset(initial: Map<String, T>) {
        dirty = false
        newKeys.clear()
        dirtyStatus.clear()
        initialValues.putAll(initial)
    }

}