package com.joelhorrocks.paperclip.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HistoryRepository {
    fun getAllHistoryEntries(): Flow<List<HistoryEntry>>
    suspend fun insertHistoryEntries(vararg historyEntries: HistoryEntry)

    suspend fun deleteHistoryEntry(historyEntry: HistoryEntry)

    suspend fun clearHistory()
}