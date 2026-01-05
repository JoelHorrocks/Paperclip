package com.joelhorrocks.paperclip.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.collections.map

class HistoryRepository @Inject constructor(private val historyDao: HistoryDao) {
    fun getAllHistoryEntries(): Flow<List<HistoryEntry>> = historyDao.getAll().map { entities -> entities.map{ HistoryEntry(it.id, it.url, it.title, it.timestamp) } }

    suspend fun insertHistoryEntries(vararg historyEntries: HistoryEntry) {
        historyDao.insertAll(*historyEntries.map { HistoryEntryEntity(url = it.url, title = it.title, timestamp = it.timestamp) }.toTypedArray())
    }

    suspend fun deleteHistoryEntry(historyEntry: HistoryEntry) {
        historyEntry.id?.let {
            historyDao.delete(HistoryEntryEntity(historyEntry.id, historyEntry.url, historyEntry.title, historyEntry.timestamp))
        }
        // TODO: handle null id
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }
}