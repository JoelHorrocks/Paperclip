package com.joelhorrocks.paperclip.ml.remote

import com.joelhorrocks.paperclip.ml.Language
import com.joelhorrocks.paperclip.ml.TranslationModel
import com.joelhorrocks.paperclip.ml.TranslationModelDownloadStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteTranslationModel(
    val id: Int,
    val name: String,
    @SerialName("from_language")
    val fromLanguage: Language,
    @SerialName("to_language")
    val toLanguage: Language,
    val size: Long,
    val url: String?,
)

fun RemoteTranslationModel.toDomain(): TranslationModel = TranslationModel(
    this.id,
    this.name,
    this.fromLanguage,
    this.toLanguage,
    this.size,
    TranslationModelDownloadStatus.Available(this.url ?: "")
)