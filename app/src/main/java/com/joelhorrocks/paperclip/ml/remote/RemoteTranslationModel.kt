package com.joelhorrocks.paperclip.ml.remote

import com.joelhorrocks.paperclip.ml.Language
import com.joelhorrocks.paperclip.ml.TranslationModel
import com.joelhorrocks.paperclip.ml.TranslationModelDownloadStatus

data class RemoteTranslationModel(
    val id: Int,
    val name: String,
    val fromLanguage: Language,
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