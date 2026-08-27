package com.joelhorrocks.paperclip.ml.remote

import com.joelhorrocks.paperclip.HttpClientProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class TranslationModelRemoteDataSource @Inject constructor(httpClientProvider: HttpClientProvider) {
    private val httpClient = httpClientProvider.httpClient

    suspend fun getModels(): List<RemoteTranslationModel> {
        return httpClient.get("https://paperclip.storeimg.com/models.json").body()
    }
}