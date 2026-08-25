package com.joelhorrocks.paperclip.ml

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TranslationModelRepositoryImpl(
    private val translationModelRemoteDataSource: TranslationModelRemoteDataSource
): TranslationModelRepository {
    private val _models = MutableStateFlow(listOf<TranslationModel>())
    override val models = _models.asStateFlow()

    override suspend fun fetchModels() {
        _models.value = translationModelRemoteDataSource.getModels().map {
            TranslationModel(
                it.id,
                it.name,
                it.fromLanguage,
                it.toLanguage,
                it.size,
                TranslationModelDownloadStatus.Available(it.url ?: "")
            )
        }
    }
}