package com.joelhorrocks.paperclip.ml

import com.joelhorrocks.paperclip.ml.local.TranslationModelEntity
import com.joelhorrocks.paperclip.ml.local.TranslationModelLocalDataSource
import com.joelhorrocks.paperclip.ml.local.toDomain
import com.joelhorrocks.paperclip.ml.remote.RemoteTranslationModel
import com.joelhorrocks.paperclip.ml.remote.TranslationModelRemoteDataSource
import com.joelhorrocks.paperclip.ml.remote.toDomain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.io.File
import javax.inject.Inject

class TranslationModelRepositoryImpl @Inject constructor(
    private val translationModelLocalDataSource: TranslationModelLocalDataSource,
    private val translationModelRemoteDataSource: TranslationModelRemoteDataSource,
    private val httpClient: HttpClient,
    private val externalScope: CoroutineScope
): TranslationModelRepository {
    private val remoteModels = MutableStateFlow(listOf<RemoteTranslationModel>())
    // TODO: remove
    private val downloadingModels = MutableStateFlow(listOf<TranslationModel>())
    // TODO: merge lists
    // TODO: stateIn / shareIn
    override val models = combine(
        remoteModels,
        translationModelLocalDataSource.getModels(),
        downloadingModels) { remote, local, downloading ->
        val localModels = local.map { it.toDomain() }
        val remoteModels = remote.map { it.toDomain() }

        // TODO: some proper enforcement that IDs are not duplicated anywhere
        (localModels + remoteModels.filter { localModels.none { local -> local.id == it.id } && downloading.none { downloading -> downloading.id == it.id } } + downloading).sortedBy { it.id }
    }

    override suspend fun fetchModels() {
        remoteModels.value = translationModelRemoteDataSource.getModels()
    }

    // TODO: use UIDT?
    // TODO: move to local datasource?? where do we put downloading models?
    // keep in memory in the repository for now
    // TODO: split this up into smaller functions, inject dispatcher?
    override suspend fun downloadModel(id: Int) {
        val model = remoteModels.value.firstOrNull { it.id == id }

        // TODO: error handling
        if (model != null && model.url != null) {
            downloadingModels.value += model.toDomain().copy(downloadStatus = TranslationModelDownloadStatus.Downloading(0f))

            externalScope.launch {
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile("files", "index")
                }
                val stream = withContext(Dispatchers.IO) {
                    file.outputStream().asSink()
                }
                val bufferSize: Long = 1024 * 1024

                httpClient.prepareGet(model.url).execute { httpResponse ->
                    val channel: ByteReadChannel = httpResponse.body()
                    var count = 0L
                    stream.use {
                        while (!channel.exhausted()) {
                            withContext(Dispatchers.IO) {
                                val chunk = channel.readRemaining(bufferSize)
                                count += chunk.remaining
                                chunk.transferTo(stream)
                            }
                            downloadingModels.update { translationModels ->
                                val model = translationModels.firstOrNull { it.id == id }
                                if (model != null) {
                                    translationModels.filter { it.id != id } + model.copy(
                                        downloadStatus = TranslationModelDownloadStatus.Downloading(count.toFloat() / httpResponse.contentLength()!!)
                                    )
                                } else {
                                    translationModels
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    translationModelLocalDataSource.insertAll(
                        TranslationModelEntity(
                            id = model.id,
                            name = model.name,
                            fromLanguage = model.fromLanguage,
                            toLanguage = model.toLanguage,
                            size = file.length(),
                            path = file.path
                        )
                    )
                }

                downloadingModels.update { translationModels ->
                    translationModels.filter { it.id != id }
                }
            }
            // TODO: how to handle downloads, should we do database write each time??
        }
    }

    override suspend fun deleteModel(id: Int) {
        translationModelLocalDataSource.delete(id)
    }
}