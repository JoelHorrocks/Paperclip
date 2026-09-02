package com.joelhorrocks.paperclip.ml.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class TranslationModelRemoteDataSource @Inject constructor(private val httpClient: HttpClient) {

    suspend fun getModels(): List<RemoteTranslationModel> {
        return httpClient.get("https://paperclip.storeimg.com/models.json").body()
    }
}