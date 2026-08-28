package com.joelhorrocks.paperclip.news

interface NewsRepository {
    suspend fun fetchNews(n: Int): List<Article>
    suspend fun fetchNewsFrom(n: Int, cursor: String): List<Article>
}