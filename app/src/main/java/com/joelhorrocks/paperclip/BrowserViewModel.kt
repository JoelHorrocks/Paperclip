package com.joelhorrocks.paperclip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelhorrocks.paperclip.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(private val tabController: TabController, private val settingsRepository: SettingsRepository): ViewModel() {
    data class BrowserUiState(
        val tabs: List<TabController.Tab> = emptyList(),
        val currentTabIndex: Int? = 0,
        val navBarText: String = "",
        val isLoading: Boolean = false,
        val showToolbarTooltip: Boolean = false
    ) {
        val currentTab: TabController.Tab?
            get() = if(currentTabIndex != null) tabs.getOrNull(currentTabIndex) else null
        val currentUrl: String
            get() = if(currentTabIndex != null) tabs.getOrNull(currentTabIndex)?.currentUrl ?: "" else ""
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
                        navBarText = currentIndex?.let {
                            index -> if(tabs[index].currentUrl == HOME_URL) "" else tabs[index].currentUrl
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
        if(currentTab != null) {
            viewModelScope.launch {
                tabController.loadUrl(currentTab, navBarText)
            }
        }
    }

    fun loadUrl(url: String) {
        val currentTab = _uiState.value.currentTab
        if(currentTab != null) {
            viewModelScope.launch {
                tabController.loadUrl(currentTab, url)
            }
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
        if(currentTab != null) {
            viewModelScope.launch {
                tabController.goBack(currentTab)
            }
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
}