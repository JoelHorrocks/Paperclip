package com.joelhorrocks.paperclip.news

interface NewsRepository {
    suspend fun fetchLatestNews(): List<Article>
}