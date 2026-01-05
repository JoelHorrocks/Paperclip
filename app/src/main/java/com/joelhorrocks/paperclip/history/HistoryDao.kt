package com.joelhorrocks.paperclip.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history order by timestamp DESC")
    fun getAll(): Flow<List<HistoryEntryEntity>>

    @Insert
    suspend fun insertAll(vararg historyEntry: HistoryEntryEntity)

    @Delete
    suspend fun delete(historyEntry: HistoryEntryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}