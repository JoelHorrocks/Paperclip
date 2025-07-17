package com.joelhorrocks.paperclip.shortcuts

import androidx.compose.ui.res.painterResource
import com.joelhorrocks.paperclip.R
import com.joelhorrocks.paperclip.news.Article
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: add datasources - for now they are fake
class ShortcutsRepository() {
    private val mockShortcuts = listOf(
        Shortcut(
            id = 0,
            url = "https://google.com",
            name = "Google"
        ),
        Shortcut(
            id = 1,
            url = "https://youtube.com",
            name = "YouTube"
        ),
        Shortcut(
            id = 2,
            url = "https://amazon.com",
            name = "Amazon"
        ),
        Shortcut(
            id = 3,
            url = "https://github.com",
            name = "GitHub"
        )
    )

    suspend fun fetchShortcuts(): List<Shortcut> {
        // TODO: remove loading simulation delay
        delay(100)
        return mockShortcuts
    }
}