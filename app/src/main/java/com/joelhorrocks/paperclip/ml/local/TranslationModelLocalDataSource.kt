package com.joelhorrocks.paperclip.ml.local

import com.joelhorrocks.paperclip.history.HistoryDao
import com.joelhorrocks.paperclip.ml.Language
import com.joelhorrocks.paperclip.ml.remote.RemoteTranslationModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TranslationModelLocalDataSource @Inject constructor(private val translationModelDao: TranslationModelDao) {
    fun getModels(): Flow<List<TranslationModelEntity>> {
        return translationModelDao.getAll()
    }

    suspend fun insertAll(vararg translationModelEntities: TranslationModelEntity) {
        translationModelDao.insertAll(*translationModelEntities)
    }

    suspend fun update(translationModelEntity: TranslationModelEntity) {
        translationModelDao.update(translationModelEntity)
    }

    suspend fun delete(translationModelEntity: TranslationModelEntity) {
        translationModelDao.delete(translationModelEntity)
    }

    suspend fun delete(int: Int) {
        translationModelDao.delete(int)
    }
}