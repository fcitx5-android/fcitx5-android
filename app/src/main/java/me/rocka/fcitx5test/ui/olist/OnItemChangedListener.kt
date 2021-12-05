package me.rocka.fcitx5test.ui.olist


/**
 * Functions are called after the container changed
 */
interface OnItemChangedListener<T> {

    fun onItemSwapped(fromIdx: Int, toIdx: Int, item: T)

    fun onItemAdded(idx: Int, item: T)

    fun onItemRemoved(idx: Int, item: T)

    fun onItemUpdated(idx: Int, old: T, new: T)

}