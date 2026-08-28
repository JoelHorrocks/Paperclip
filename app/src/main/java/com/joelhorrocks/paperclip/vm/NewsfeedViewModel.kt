package com.joelhorrocks.paperclip.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelhorrocks.paperclip.news.Article
import com.joelhorrocks.paperclip.news.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsfeedViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
) : ViewModel() {

    data class NewsfeedUiState(
        val articleLoadingState: ArticleLoadingState = ArticleLoadingState.LOADING,
        val articleList: List<Article> = listOf(),
        val cursor: String? = null,
    )

    private val _uiState = MutableStateFlow(NewsfeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchArticleBatch()
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
            // TODO: proper batch system
            val articles = if(_uiState.value.cursor == null) {
                newsRepository.fetchNews(10)
            } else {
                newsRepository.fetchNewsFrom(10, _uiState.value.cursor!!)
            }

            if(articles.isEmpty()) {
                _uiState.update {
                    it.copy(
                        articleLoadingState = ArticleLoadingState.END
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        articleLoadingState = ArticleLoadingState.SUCCESS,
                        articleList = it.articleList + articles,
                        cursor = articles.last().id
                    )
                }
            }
        }
    }
}