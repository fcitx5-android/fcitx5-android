package org.fcitx.fcitx5.android.data.clipboard.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClipboardEntry::class], version = 1)
abstract class ClipboardDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
}