package com.joelhorrocks.paperclip.ml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TranslationModelRepository {
    val models: Flow<List<TranslationModel>>
    suspend fun fetchModels()
}