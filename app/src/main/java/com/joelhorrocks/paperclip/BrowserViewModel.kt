package com.joelhorrocks.paperclip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.news.Article
import com.joelhorrocks.paperclip.news.NewsRepository
import com.joelhorrocks.paperclip.settings.SettingsRepository
import com.joelhorrocks.paperclip.shortcuts.Shortcut
import com.joelhorrocks.paperclip.shortcuts.ShortcutsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import javax.inject.Inject

enum class ArticleLoadingState {
    LOADING, SUCCESS, ERROR
}

enum class ShortcutsLoadingState {
    LOADING, SUCCESS, ERROR
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabController: TabController,
    private val settingsRepository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val shortcutsRepository: ShortcutsRepository
) : ViewModel() {

    data class BrowserUiState(
        val tabs: List<Tab> = emptyList(),
        val currentTabIndex: Int? = 0,
        val navBarText: String = "",
        val isLoading: Boolean = false,
        val showToolbarTooltip: Boolean = false,
        val articleLoadingState: ArticleLoadingState = ArticleLoadingState.LOADING,
        val articleList: List<Article> = listOf(),
        val shortcutsLoadingState: ShortcutsLoadingState = ShortcutsLoadingState.LOADING,
        val shortcutList: List<Shortcut> = listOf()
    ) {
        val currentTab: Tab?
            get() = if (currentTabIndex != null) tabs.getOrNull(currentTabIndex) else null
        val currentUrl: String
            get() = if (currentTabIndex != null) tabs.getOrNull(currentTabIndex)?.currentUrl
                ?: "" else ""
    }

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState = _uiState.asStateFlow()

    val prompts = tabController.prompts

    init {
        viewModelScope.launch {
            combine(
                tabController.tabs,
                tabController.currentTabIndex,
                settingsRepository.showDrawerTooltip
            ) { tabs, currentIndex, showDrawerTooltip ->
                _uiState.update {
                    it.copy(
                        tabs = tabs,
                        currentTabIndex = currentIndex,
                        navBarText = currentIndex?.let { index ->
                            if (tabs[index].currentUrl == HOME_URL) "" else tabs[index].currentUrl
                        } ?: "",
                        isLoading = currentIndex?.let { index -> tabs[index].isLoading } ?: false,
                        showToolbarTooltip = showDrawerTooltip
                    )
                }
            }.collect()
        }
    }

    // TODO: when clicking out of toolbar, text should return to currentUrl
    // TODO: rename function to reflect changing navbar text
    fun updateUrl(url: String) {
        _uiState.update {
            it.copy(
                navBarText = url
            )
        }
    }

    fun submitUrl() {
        val currentTab = _uiState.value.currentTab
        val navBarText = _uiState.value.navBarText
        // TODO: consider this behaviour, in some cases e.g. window.open(), navbar is blank for a period of time
        _uiState.update {
            it.copy(
                navBarText = ""
            )
        }
        if (currentTab != null) {
            tabController.loadUrl(
                currentTab,
                if (navBarText.startsWith("data:") || ((navBarText.contains(":") || navBarText.contains(
                        "."
                    )) && !navBarText.contains(" "))
                ) navBarText else SEARCH_BASE_URI + navBarText
            )
        }
    }

    fun loadUrl(url: String) {
        val currentTab = _uiState.value.currentTab
        if (currentTab != null) {
            tabController.loadUrl(currentTab, url)
        }
    }

    fun selectTab(index: Int) {
        tabController.selectTab(index)
    }

    fun closeTab(index: Int) {
        tabController.closeTab(index)
    }

    fun createTab() {
        tabController.createTab()
    }

    fun goBack() {
        val currentTab = _uiState.value.currentTab
        if (currentTab != null) {
            tabController.goBack(currentTab)
        }
    }

    fun setShowToolbarTooltip(shown: Boolean) {
        _uiState.update {
            it.copy(
                showToolbarTooltip = shown
            )
        }
        viewModelScope.launch {
            settingsRepository.setShowDrawerTooltip(shown)
        }
    }

    fun fetchArticles() {
        viewModelScope.launch {
            // TODO: errorhandling
            val articles = newsRepository.fetchLatestNews()
            _uiState.update {
                it.copy(
                    articleLoadingState = ArticleLoadingState.SUCCESS,
                    articleList = articles
                )
            }
        }
    }

    fun fetchShortcuts() {
        viewModelScope.launch {
            val shortcuts = shortcutsRepository.fetchShortcuts()
            _uiState.update {
                it.copy(
                    shortcutsLoadingState = ShortcutsLoadingState.SUCCESS,
                    shortcutList = shortcuts
                )
            }
        }
    }
}