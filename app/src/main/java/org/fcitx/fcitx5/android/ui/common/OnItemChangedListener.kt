/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.common

/**
 * Functions are called after the container changed
 */
interface OnItemChangedListener<T> {

    fun onItemSwapped(fromIdx: Int, toIdx: Int, item: T) {}

    fun onItemAdded(idx: Int, item: T) {}

    fun onItemRemoved(idx: Int, item: T) {}

    fun onItemUpdated(idx: Int, old: T, new: T) {}

    fun onItemRemovedBatch(indexed: List<Pair<Int, T>>) {}

    // only on multiselect
    fun batchRemove(indexed: List<Pair<Int, T>>) {
        indexed.forEach { onItemRemoved(it.first, it.second) }
    }

    companion object {
        /**
         * Merge two listeners
         */
        fun <T> merge(l1: OnItemChangedListener<T>, l2: OnItemChangedListener<T>) =
            object : OnItemChangedListener<T> {
                override fun onItemSwapped(fromIdx: Int, toIdx: Int, item: T) {
                    l1.onItemSwapped(fromIdx, toIdx, item)
                    l2.onItemSwapped(fromIdx, toIdx, item)
                }

                override fun onItemAdded(idx: Int, item: T) {
                    l1.onItemAdded(idx, item)
                    l2.onItemAdded(idx, item)
                }

                override fun onItemRemoved(idx: Int, item: T) {
                    l1.onItemRemoved(idx, item)
                    l2.onItemRemoved(idx, item)
                }

                override fun onItemRemovedBatch(indexed: List<Pair<Int, T>>) {
                    l1.onItemRemovedBatch(indexed)
                    l2.onItemRemovedBatch(indexed)
                }

                override fun onItemUpdated(idx: Int, old: T, new: T) {
                    l1.onItemUpdated(idx, old, new)
                    l2.onItemUpdated(idx, old, new)
                }

            }
    }
}