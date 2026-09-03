package com.joelhorrocks.paperclip.ml

import com.joelhorrocks.paperclip.ml.local.TranslationModelEntity
import com.joelhorrocks.paperclip.ml.local.TranslationModelLocalDataSource
import com.joelhorrocks.paperclip.ml.local.toDomain
import com.joelhorrocks.paperclip.ml.remote.RemoteTranslationModel
import com.joelhorrocks.paperclip.ml.remote.TranslationModelRemoteDataSource
import com.joelhorrocks.paperclip.ml.remote.toDomain
import com.joelhorrocks.paperclip.utils.getSize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.inject.Inject

class TranslationModelRepositoryImpl @Inject constructor(
    private val translationModelLocalDataSource: TranslationModelLocalDataSource,
    private val translationModelRemoteDataSource: TranslationModelRemoteDataSource,
    private val httpClient: HttpClient,
    private val externalScope: CoroutineScope,
    private val modelDirectory: Path
): TranslationModelRepository {
    private val remoteModels = MutableStateFlow(listOf<RemoteTranslationModel>())
    // TODO: remove
    private val downloadingModels = MutableStateFlow(listOf<TranslationModel>())
    // TODO: keeping this here since we don't want anything else cancelling jobs, think about this
    private val downloadingJobs = mutableMapOf<Int, Job>()
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

    override fun cancelDownload(id: Int){
        // TODO: handle ID not valid
        // TODO: should we do cleanup in CancellationException handler?
        if(downloadingJobs.containsKey(id)) {
            downloadingJobs[id]?.cancel()
            downloadingJobs.remove(id)

            downloadingModels.update { translationModels ->
                translationModels.filter { it.id != id }
            }
            // TODO: clean up file?
        }
    }

    // TODO: use UIDT?
    // TODO: move to local datasource?? where do we put downloading models?
    // keep in memory in the repository for now
    // TODO: split this up into smaller functions, inject dispatcher?
    override fun downloadModel(id: Int) {
        val model = remoteModels.value.firstOrNull { it.id == id }

        // TODO: error handling
        if (model != null && model.url != null) {
            downloadingModels.update {
                it + model.toDomain().copy(downloadStatus = TranslationModelDownloadStatus.Downloading(0f))
            }

            val job = externalScope.launch(Dispatchers.IO) {
                val file = File.createTempFile("files", "index")
                val stream = file.outputStream().asSink()

                // TODO: copy over once done? think about file unzip process
                val targetDirectory = File(modelDirectory.toFile(), model.id.toString())

                try {
                    val bufferSize: Long = 1024 * 1024

                    httpClient.prepareGet(model.url) {
                        timeout {
                            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                            socketTimeoutMillis = 30000
                        }
                    }.execute { httpResponse ->
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
                                            downloadStatus = TranslationModelDownloadStatus.Downloading(
                                                count.toFloat() / httpResponse.contentLength()!!
                                            )
                                        )
                                    } else {
                                        translationModels
                                    }
                                }
                            }
                        }
                    }

                    // copy file to app storage and unzip
                    // TODO: error if file is not a zip

                    if(!targetDirectory.exists()) {
                        targetDirectory.mkdirs()
                    }

                    ZipFile(file).use { zip ->
                        zip.entries().asSequence().forEach { entry ->
                            val filePath = File(targetDirectory, entry.name)

                            val canonicalDestination = targetDirectory.canonicalPath
                            val canonicalFile = filePath.canonicalPath

                            if(!canonicalFile.startsWith(canonicalDestination + File.separator)) {
                                throw IllegalArgumentException("Entry is outside of the target dir: $filePath")
                            }

                            zip.getInputStream(entry).use { input ->
                                if(entry.isDirectory) {
                                    filePath.mkdirs()
                                } else {
                                    filePath.parentFile?.mkdirs()
                                    filePath.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    }

                    file.delete()

                    translationModelLocalDataSource.insertAll(
                        TranslationModelEntity(
                            id = model.id,
                            name = model.name,
                            fromLanguage = model.fromLanguage,
                            toLanguage = model.toLanguage,
                            size = targetDirectory.getSize(),
                        )
                    )

                    downloadingModels.update { translationModels ->
                        translationModels.filter { it.id != id }
                    }
                }
                catch (_: Exception) {
                    // TODO: handle coroutine cancellation
                    currentCoroutineContext().ensureActive()

                    stream.close()
                    file.delete()
                    targetDirectory.deleteRecursively()

                    downloadingModels.update { translationModels ->
                        val model = translationModels.firstOrNull { it.id == id }
                        if (model != null) {
                            translationModels.filter { it.id != id } + model.copy(
                                downloadStatus = TranslationModelDownloadStatus.Error(DownloadError.UNKNOWN)
                            )
                        } else {
                            translationModels
                        }
                    }
                }
            }
            downloadingJobs[id] = job
            // TODO: how to handle downloads, should we do database write each time??
        }
    }

    override suspend fun deleteModel(id: Int) {
        val model = translationModelLocalDataSource.getModel(id)
        if (model != null) {
            translationModelLocalDataSource.delete(id)
            withContext(Dispatchers.IO) {
                File("$modelDirectory/${model.id}").deleteRecursively()
            }
        }
    }
}