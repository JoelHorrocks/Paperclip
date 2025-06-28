package com.joelhorrocks.paperclip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(private val tabController: TabController): ViewModel() {
    data class BrowserUiState(
        val tabs: List<TabController.Tab> = emptyList(),
        val currentTabIndex: Int? = 0,
        val currentUrl: String = "",
        val navBarText: String = "",
        val isLoading: Boolean = false
    ) {
        val currentTab: TabController.Tab?
            get() = if(currentTabIndex != null) tabs[currentTabIndex] else null
    }

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tabController.tabs,
                tabController.currentTabIndex
            ) { tabs, currentIndex ->
                _uiState.update {
                    it.copy(
                        tabs = tabs,
                        currentTabIndex = currentIndex,
                        currentUrl = currentIndex?.let { index -> tabs[index].currentUrl } ?: "",
                        navBarText = currentIndex?.let { index -> tabs[index].currentUrl } ?: "",
                        isLoading = currentIndex?.let { index -> tabs[index].isLoading } ?: false
                    )
                }
            }.collect()
        }
    }

    fun updateUrl(url: String) {
        _uiState.update {
            it.copy(
                navBarText = url
            )
        }
    }

    fun submitUrl() {
        val currentTab = _uiState.value.currentTab
        if(currentTab != null) {
            viewModelScope.launch {
                tabController.loadUrl(currentTab, _uiState.value.navBarText)
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
}