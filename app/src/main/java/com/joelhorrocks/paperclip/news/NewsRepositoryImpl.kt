package com.joelhorrocks.paperclip.news

import com.joelhorrocks.paperclip.HttpClientProvider
import com.joelhorrocks.paperclip.R
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

// TODO: local datasource (caching?)
class NewsRepositoryImpl @Inject constructor(httpClientProvider: HttpClientProvider): NewsRepository {
    private val httpClient = httpClientProvider.httpClient

    // TODO: merge fetchNews and fetchNewsFrom?
    override suspend fun fetchNews(n: Int): List<Article> {
        return httpClient.get("https://storeimg.com/news/") {
            parameter("limit", n)
        }.body<List<Article>>()
    }

    override suspend fun fetchNewsFrom(n: Int, cursor: String): List<Article> {
        return httpClient.get("https://storeimg.com/news/") {
            parameter("limit", n)
            parameter("after", cursor)
        }.body<List<Article>>()
    }
}