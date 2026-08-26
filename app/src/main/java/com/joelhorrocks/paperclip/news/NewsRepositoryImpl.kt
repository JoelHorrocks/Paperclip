package com.joelhorrocks.paperclip.news

import com.joelhorrocks.paperclip.HttpClientProvider
import com.joelhorrocks.paperclip.R
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

// TODO: local datasource (caching?)
class NewsRepositoryImpl @Inject constructor(httpClientProvider: HttpClientProvider): NewsRepository {
    private val httpClient = httpClientProvider.httpClient

    override suspend fun fetchLatestNews(): List<Article> {
        return httpClient.get("https://paperclip.storeimg.com/news.json").body()
    }
}