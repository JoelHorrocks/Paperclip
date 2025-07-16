package com.joelhorrocks.paperclip.news

import java.util.Date

data class Article(
    val id: Int,
    val headline: String,
    val description: String,
    val publicationDate: Date,
    val publisher: String,
    val readTimeMin: Int,
    // TODO: image - URL?
    val imageResource: Int,
    val url: String
)
