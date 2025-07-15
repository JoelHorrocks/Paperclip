package com.joelhorrocks.paperclip

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Screen: NavKey {
    @Serializable
    data object Setup : Screen()
    @Serializable
    data object Home : Screen()
    @Serializable
    data object Newsfeed : Screen()
    @Serializable
    data object Settings : Screen()
}