package com.joelhorrocks.paperclip.ml

import kotlinx.coroutines.Job

data class TranslationModel (
    val id: Int,
    val name: String,
    val fromLanguage: Language,
    val toLanguage: Language,
    val size: Long,
    val downloadStatus: TranslationModelDownloadStatus
)

sealed interface TranslationModelDownloadStatus{
    data class Available(val url: String): TranslationModelDownloadStatus
    data class Downloading(val progress: Float): TranslationModelDownloadStatus
    data class Downloaded(val path: String): TranslationModelDownloadStatus
    data class Error(val error: DownloadError): TranslationModelDownloadStatus
}

enum class DownloadError {
    UNKNOWN
}

