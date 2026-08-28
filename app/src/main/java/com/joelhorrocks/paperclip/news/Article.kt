package com.joelhorrocks.paperclip.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Article(
    val id: String,
    val headline: String,
    val description: String,
    @SerialName("publication_date")
    val publicationDate: Instant,
    val publisher: String,
    @SerialName("reading_time_min")
    val readTimeMin: Int?,
    @SerialName("image_url")
    val imageUrl: String?,
    val url: String
)
