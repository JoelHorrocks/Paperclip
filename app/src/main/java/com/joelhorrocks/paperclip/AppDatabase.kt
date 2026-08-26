package com.joelhorrocks.paperclip

import androidx.room.Database
import androidx.room.RoomDatabase
import com.joelhorrocks.paperclip.history.HistoryDao
import com.joelhorrocks.paperclip.history.HistoryEntryEntity
import com.joelhorrocks.paperclip.ml.local.TranslationModelDao
import com.joelhorrocks.paperclip.ml.local.TranslationModelEntity
import com.joelhorrocks.paperclip.shortcuts.ShortcutDao
import com.joelhorrocks.paperclip.shortcuts.ShortcutEntity

@Database(entities = [ShortcutEntity::class, HistoryEntryEntity::class, TranslationModelEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao
    abstract fun historyDao(): HistoryDao
    abstract fun translationModelDao(): TranslationModelDao
}