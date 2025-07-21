package com.joelhorrocks.paperclip.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.SEARCH_BASE_URI
import com.joelhorrocks.paperclip.TabController
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

@HiltViewModel
class NewsfeedViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
) : ViewModel() {

    data class NewsfeedUiState(
        val articleLoadingState: ArticleLoadingState = ArticleLoadingState.LOADING,
        val articleList: List<Article> = listOf()
    )

    private val _uiState = MutableStateFlow(NewsfeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fetchArticleBatch()
        }
    }

    // TODO: share with BrowserViewModel?
    // TODO: tune number of articles
    fun fetchArticleBatch() {
        _uiState.update {
            it.copy(
                articleLoadingState = ArticleLoadingState.LOADING
            )
        }
        viewModelScope.launch {
            // TODO: errorhandling
            val articles = newsRepository.fetchLatestNews()
            _uiState.update {
                it.copy(
                    articleLoadingState = ArticleLoadingState.SUCCESS,
                    articleList = _uiState.value.articleList + articles
                )
            }
        }
    }
}