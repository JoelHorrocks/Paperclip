package com.joelhorrocks.paperclip.news

import java.util.Date

data class Article(
    val id: Int,
    val headline: String,
    val description: String,
    val publicationDate: Date,
    val readTimeMin: Int,
    val url: String
)
