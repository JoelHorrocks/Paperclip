package com.joelhorrocks.paperclip.shortcuts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcut")
    fun getAll(): Flow<List<ShortcutEntity>>

    @Insert
    suspend fun insertAll(vararg shortcuts: ShortcutEntity)

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)
}