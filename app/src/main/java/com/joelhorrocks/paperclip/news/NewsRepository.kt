package com.joelhorrocks.paperclip.news

interface NewsRepository {
    suspend fun fetchLatestNews(n: Int): List<Article>
}