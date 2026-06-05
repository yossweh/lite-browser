package com.litebrowser.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.litebrowser.database.bookmarks.Bookmark
import com.litebrowser.database.bookmarks.dao.BookmarkDao
import com.litebrowser.database.hist.History
import com.litebrowser.database.hist.dao.HistoryDao
import com.litebrowser.database.shortcuts.Shortcut
import com.litebrowser.database.shortcuts.dao.ShortcutDao

@Database(
    entities = [Bookmark::class, History::class, Shortcut::class],
    version = 1,
    exportSchema = false
)
abstract class ESearchDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        private var instance: ESearchDatabase? = null

        @Synchronized
        fun getInstance(ctx: Context): ESearchDatabase {
            if (instance == null)
                instance = Room.databaseBuilder(
                    ctx.applicationContext, ESearchDatabase::class.java,
                    "room_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            return instance!!
        }
    }
}