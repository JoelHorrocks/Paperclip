package com.joelhorrocks.paperclip.shortcuts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ShortcutRepository {
    fun getAllShortcuts(): Flow<List<Shortcut>>

    suspend fun insertShortcuts(vararg shortcuts: Shortcut)

    suspend fun deleteShortcut(shortcut: Shortcut)
}