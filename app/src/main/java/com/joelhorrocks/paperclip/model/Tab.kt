package com.joelhorrocks.paperclip.model

import org.mozilla.geckoview.GeckoSession
import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    //val geckoSession: GeckoSession,
    val isLoading: Boolean = false,
    val currentUrl: String = ""
)