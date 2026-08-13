package com.joelhorrocks.paperclip.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val isLoading: Boolean = false,
    val loadingPercentage: Float = 0.0F,
    val currentUrl: String = "",
    val sessionSnapshot: String? = null
)