package com.joelhorrocks.paperclip.ml

import com.joelhorrocks.paperclip.ml.local.TranslationModelEntity
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
        translationModelLocalDataSource.getModels()) { remote, local ->
        val localModels = local.map { it.toDomain() }
        val remoteModels = remote.map { it.toDomain() }

        localModels + remoteModels.filter { localModels.none { local -> local.id == it.id } }
    }

    override suspend fun fetchModels() {
        remoteModels.value = translationModelRemoteDataSource.getModels()
    }

    override suspend fun markDownloaded(id: Int) {
        val model = remoteModels.value.firstOrNull { it.id == id }

        // TODO: handle null
        if (model != null) {
            translationModelLocalDataSource.insertAll(
                TranslationModelEntity(
                    id = model.id,
                    name = model.name,
                    fromLanguage = model.fromLanguage,
                    toLanguage = model.toLanguage,
                    size = model.size,
                    downloading = false
                )
            )
        }
    }

    override suspend fun deleteModel(id: Int) {
        translationModelLocalDataSource.delete(id)
    }
}