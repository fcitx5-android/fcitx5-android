/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import kotlin.properties.Delegates

class NaiveDustman<T> {

    private val initialValues = mutableMapOf<String, T>()

    private val dirtyStatus = mutableSetOf<String>()

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
        if (boolean) {
            dirtyStatus.add(key)
        } else {
            dirtyStatus.remove(key)
        }
        dirty = dirtyStatus.isNotEmpty()
    }

    fun addOrUpdate(key: String, value: T) {
        if (initialValues.containsKey(key)) {
            updateDirtyStatus(key, initialValues[key] != value)
        } else {
            updateDirtyStatus(key, true)
        }
    }

    fun remove(key: String) {
        updateDirtyStatus(key, initialValues.containsKey(key))
    }

    fun reset(initial: Map<String, T>) {
        dirty = false
        dirtyStatus.clear()
        initialValues.putAll(initial)
    }

}