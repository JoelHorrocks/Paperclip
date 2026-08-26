package com.joelhorrocks.paperclip.ml

import com.joelhorrocks.paperclip.ml.local.TranslationModelLocalDataSource
import com.joelhorrocks.paperclip.ml.local.toDomain
import com.joelhorrocks.paperclip.ml.remote.RemoteTranslationModel
import com.joelhorrocks.paperclip.ml.remote.TranslationModelRemoteDataSource
import com.joelhorrocks.paperclip.ml.remote.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class TranslationModelRepositoryImpl(
    private val translationModelLocalDataSource: TranslationModelLocalDataSource,
    private val translationModelRemoteDataSource: TranslationModelRemoteDataSource
): TranslationModelRepository {
    private val remoteModels = MutableStateFlow(listOf<RemoteTranslationModel>())
    // TODO: merge lists
    // TODO: stateIn / shareIn
    override val models = combine(
        remoteModels,
        translationModelLocalDataSource.getModels()) { first, second ->
        first.map { it.toDomain() } + second.map { it.toDomain() }
    }

    override suspend fun fetchModels() {
        remoteModels.value = translationModelRemoteDataSource.getModels()
    }
}