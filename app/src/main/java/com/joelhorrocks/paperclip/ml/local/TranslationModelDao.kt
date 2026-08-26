package com.joelhorrocks.paperclip.ml.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.joelhorrocks.paperclip.history.HistoryEntryEntity
import com.joelhorrocks.paperclip.ml.TranslationModel
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationModelDao {
    @Query("SELECT * FROM translationmodels")
    fun getAll(): Flow<List<TranslationModelEntity>>

    @Insert
    suspend fun insertAll(vararg translationModelEntities: TranslationModelEntity)

    @Delete
    suspend fun delete(translationModelEntity: TranslationModelEntity)

    @Query("DELETE FROM translationmodels WHERE id = :int")
    suspend fun delete(int: Int)

    @Query("DELETE FROM translationmodels")
    suspend fun clearAll()
}