package com.joelhorrocks.paperclip.ml

import android.util.Log
import com.joelhorrocks.paperclip.HttpClientProvider
import com.joelhorrocks.paperclip.ml.local.TranslationModelEntity
import com.joelhorrocks.paperclip.ml.local.TranslationModelLocalDataSource
import com.joelhorrocks.paperclip.ml.local.toDomain
import com.joelhorrocks.paperclip.ml.remote.RemoteTranslationModel
import com.joelhorrocks.paperclip.ml.remote.TranslationModelRemoteDataSource
import com.joelhorrocks.paperclip.ml.remote.toDomain
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.none
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.io.File
import javax.inject.Inject

class TranslationModelRepositoryImpl(
    private val translationModelLocalDataSource: TranslationModelLocalDataSource,
    private val translationModelRemoteDataSource: TranslationModelRemoteDataSource,
    private val httpClientProvider: HttpClientProvider
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
    override suspend fun downloadModel(id: Int) {
        val model = remoteModels.value.firstOrNull { it.id == id }

        // TODO: error handling
        if (model != null && model.url != null) {
            downloadingModels.value += model.toDomain().copy(downloadStatus = TranslationModelDownloadStatus.Downloading(0f))

            val file = withContext(Dispatchers.IO) {
                File.createTempFile("files", "index")
            }
            val stream = file.outputStream().asSink()
            val bufferSize: Long = 1024 * 1024

            val client = httpClientProvider.httpClient

            client.prepareGet(model.url).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                var count = 0L
                stream.use {
                    while (!channel.exhausted()) {
                        val chunk = channel.readRemaining(bufferSize)
                        count += chunk.remaining

                        chunk.transferTo(stream)
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

            downloadingModels.update { translationModels ->
                translationModels.filter { it.id != id }
            }

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

            Log.d("TAG", "A file saved to ${file.path}")

            // TODO: how to handle downloads, should we do database write each time??
        }
    }

    override suspend fun deleteModel(id: Int) {
        translationModelLocalDataSource.delete(id)
    }
}