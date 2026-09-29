package com.joelhorrocks.paperclip.news

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

// TODO: local datasource (caching?)
class NewsRepositoryImpl @Inject constructor(private val httpClient: HttpClient): NewsRepository {

    // TODO: merge fetchNews and fetchNewsFrom?
    // TODO: proper error handling
    override suspend fun fetchNews(n: Int): List<Article> {
        return try {
            httpClient.get("https://storeimg.com/news/") {
                parameter("limit", n)
            }.body<List<Article>>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchNewsFrom(n: Int, cursor: String): List<Article> {
        return try {
            httpClient.get("https://storeimg.com/news/") {
                parameter("limit", n)
                parameter("after", cursor)
            }.body<List<Article>>()
        } catch (_: Exception) {
            emptyList()
        }
    }
}