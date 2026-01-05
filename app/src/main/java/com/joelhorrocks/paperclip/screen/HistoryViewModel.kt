package com.joelhorrocks.paperclip.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.SEARCH_BASE_URI
import com.joelhorrocks.paperclip.TabController
import com.joelhorrocks.paperclip.history.HistoryEntry
import com.joelhorrocks.paperclip.history.HistoryRepository
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.news.Article
import com.joelhorrocks.paperclip.news.NewsRepository
import com.joelhorrocks.paperclip.settings.SettingsRepository
import com.joelhorrocks.paperclip.shortcuts.Shortcut
import com.joelhorrocks.paperclip.shortcuts.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: use one enum for both Article and History loading states?
enum class HistoryLoadingState {
    LOADING, SUCCESS, ERROR
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    data class HistoryUiState(
        val historyLoadingState: HistoryLoadingState = HistoryLoadingState.LOADING,
        val searchQuery: String = "",
        val historyList: List<HistoryEntry> = listOf(),
        val filteredHistoryList: List<HistoryEntry> = listOf()
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                historyLoadingState = HistoryLoadingState.LOADING
            )
        }
        viewModelScope.launch {
            // TODO: errorhandling
            val historyEntries = historyRepository.getAllHistoryEntries()
            historyEntries.collect { historyList ->
                _uiState.update {
                    it.copy(
                        historyLoadingState = HistoryLoadingState.SUCCESS,
                        historyList = historyList,
                        filteredHistoryList = historyList
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun searchHistory(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredHistoryList = it.historyList.filter { entry ->
                    entry.title?.contains(query, ignoreCase = true) ?: false ||
                    entry.url.contains(query, ignoreCase = true)
                }
            )
        }
    }
}