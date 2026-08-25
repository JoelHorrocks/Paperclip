package com.joelhorrocks.paperclip.ml

import kotlinx.coroutines.flow.StateFlow

interface TranslationModelRepository {
    val models: StateFlow<List<TranslationModel>>
    suspend fun fetchModels()
}