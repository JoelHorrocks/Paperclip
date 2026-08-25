package com.joelhorrocks.paperclip.ml

data class RemoteTranslationModel(
    val id: Int,
    val name: String,
    val fromLanguage: Language,
    val toLanguage: Language,
    val size: Long,
    val url: String?,
)
