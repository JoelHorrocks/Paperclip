package com.joelhorrocks.paperclip.ml

import kotlinx.coroutines.flow.Flow

interface TranslationModelRepository {
    val models: Flow<List<TranslationModel>>
    suspend fun fetchModels()
    suspend fun markDownloaded(id: Int)
    suspend fun deleteModel(id: Int)
}