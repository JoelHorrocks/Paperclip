package com.joelhorrocks.paperclip.screen

import android.util.Log
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
import com.joelhorrocks.paperclip.tab.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject

enum class ArticleLoadingState {
    LOADING, SUCCESS, ERROR
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabController: TabController,
    private val settingsRepository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val shortcutRepository: ShortcutRepository,
    private val tabRepository: TabRepository
) : ViewModel() {

    data class BrowserUiState(
        val tabs: List<Tab> = emptyList(),
        val currentTabIndex: Int? = 0,
        val currentSession: GeckoSession? = null,
        val navBarText: String = "",
        val isLoading: Boolean = false,
        val showToolbarTooltip: Boolean = false,
        val articleLoadingState: ArticleLoadingState = ArticleLoadingState.LOADING,
        val articleList: List<Article> = listOf(),
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
            // TODO: cache or otherwise persist when navigating away then back
            fetchArticles()
            combine(
                tabRepository.tabsState,
                tabController.sessions,
                settingsRepository.showDrawerTooltip,
                // TODO: add distinctUntilChanged() in appropriate place
                shortcutRepository.getAllShortcuts()
            ) { tabsState, sessions, showDrawerTooltip, shortcuts ->
                _uiState.update {
                    it.copy(
                        tabs = tabsState.tabs,
                        currentTabIndex = tabsState.tabs.indexOfFirst { tab -> tab.id == tabsState.currentTab },
                        currentSession = sessions[tabsState.currentTab],
                        // TODO: handle not matching
                        navBarText = if (tabsState.tabs.first { tab -> tab.id == tabsState.currentTab }.currentUrl == HOME_URL) "" else tabsState.tabs.first { tab -> tab.id == tabsState.currentTab }.currentUrl,
                        isLoading = tabsState.tabs.first { tab -> tab.id == tabsState.currentTab }.isLoading,
                        showToolbarTooltip = showDrawerTooltip,
                        shortcutList = shortcuts
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

    fun selectTab(tabId: String) {
        tabController.selectTab(tabId)
    }

    fun closeTab(tabId: String) {
        tabController.closeTab(tabId)
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

    private fun fetchArticles() {
        _uiState.update {
            it.copy(
                articleLoadingState = ArticleLoadingState.LOADING
            )
        }
        viewModelScope.launch {
            // TODO: error handling
            val articles = newsRepository.fetchLatestNews()
            _uiState.update {
                it.copy(
                    articleLoadingState = ArticleLoadingState.SUCCESS,
                    articleList = articles
                )
            }
        }
    }

    fun insertShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            shortcutRepository.insertShortcuts(shortcut)
        }
    }

    fun deleteShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            shortcutRepository.deleteShortcut(shortcut)
        }
    }

    fun saveTabs() {
        tabRepository.saveTabs()
    }

    fun loadTabs() {
        tabRepository.loadTabs()
    }
}