package com.joelhorrocks.paperclip.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Article(
    val id: Int,
    val headline: String,
    val description: String,
    @SerialName("publication_date")
    val publicationDate: Instant,
    val publisher: String,
    @SerialName("read_time_min")
    val readTimeMin: Int,
    @SerialName("image_url")
    val imageUrl: String,
    val url: String
)
