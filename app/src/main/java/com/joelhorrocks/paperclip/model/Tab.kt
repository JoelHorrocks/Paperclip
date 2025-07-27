package com.joelhorrocks.paperclip.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val isLoading: Boolean = false,
    val currentUrl: String = ""
)