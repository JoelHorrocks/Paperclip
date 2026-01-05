package com.joelhorrocks.paperclip.history

data class HistoryEntry (
    val id: Int? = null,
    val url: String,
    val title: String?,
    val timestamp: Long
)