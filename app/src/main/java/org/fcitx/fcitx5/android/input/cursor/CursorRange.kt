/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.cursor

@JvmInline
value class CursorRange private constructor(val data: IntArray) {

    constructor(start: Int = 0, end: Int = 0) : this(intArrayOf(start, end))

    val start: Int get() = data[0]
    val end: Int get() = data[1]

    fun isEmpty() = data[0] == data[1]
    fun isNotEmpty() = data[0] != data[1]

    fun clear() {
        data[0] = 0
        data[1] = 0
    }

    fun update(start: Int, end: Int) {
        if (end >= start) {
            data[0] = start
            data[1] = end
        } else {
            data[0] = end
            data[1] = start
        }
    }

    fun update(i: Int) {
        data[0] = i
        data[1] = i
    }

    fun offset(offset: Int) {
        data[0] += offset
        data[1] += offset
    }

    fun contains(other: Int): Boolean {
        return other in start..end
    }

    fun contains(other: CursorRange): Boolean {
        return start <= other.start && other.end <= end
    }

    fun rangeEquals(other: CursorRange): Boolean {
        return data[0] == other.data[0] && data[1] == other.data[1]
    }

    fun rangeEquals(otherStart: Int, otherEnd: Int = otherStart): Boolean {
        return data[0] == otherStart && data[1] == otherEnd
    }

    override fun toString() = "[$start,$end]"
}
