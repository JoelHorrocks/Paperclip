package com.joelhorrocks.paperclip.tab

import android.util.Log
import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// TODO: interface
class TabRepository(
    private val tabLocalDataSource: TabLocalDataSource
) {
    data class TabsState(
        val tabs: List<Tab> = emptyList(),
        val currentTab: String? = null
    )

    private val _tabsState = MutableStateFlow(TabsState())
    val tabsState = _tabsState.asStateFlow()

    init {
        initialize()
    }

    // Load from JSON if present, otherwise create file and initial tab
    private fun initialize(): String {
        return open(HOME_URL)
    }

    fun open(url: String): String {
        val newTab = Tab(
            currentUrl = url
        )
        _tabsState.update {
            it.copy(
                tabs = it.tabs + newTab,
                currentTab = newTab.id
            )
        }

        return newTab.id
    }

    // TODO: create tabs here or manage with just URL?
    fun insertTab(tab: Tab) {
        _tabsState.update {
            it.copy(
                tabs = it.tabs + tab
            )
        }
    }

    fun close(tabId: String) {
        val tabs = tabsState.value.tabs
        val index = tabs.indexOfFirst { it.id == tabId }
        val currentTabIndex = tabs.indexOfFirst { it.id == tabsState.value.currentTab }

        // TODO: clean up now we're using IDs instead of indexes
        if (tabs.size == 1) {
            // TODO: replace when initialize handles JSON?
            val newTabId = initialize()
            _tabsState.update {
                it.copy(
                    tabs = it.tabs.filter { tab -> tab.id == newTabId }
                )
            }
            return
        } else if (currentTabIndex == index && index == 0) {
            _tabsState.update {
                it.copy(
                    currentTab = tabs[tabs.indexOfFirst { tab -> tab.id == tabsState.value.currentTab } + 1].id
                )
            }
        } else if (currentTabIndex == index) {
            _tabsState.update {
                it.copy(
                    currentTab = tabs[tabs.indexOfFirst { tab -> tab.id == tabsState.value.currentTab } - 1].id
                )
            }
        }

        _tabsState.update {
            it.copy(
                tabs = tabs.filter { tab -> tab.id != tabId }
            )
        }
    }

    fun update(tabId: String, transform: (Tab) -> Tab) {
        val tabs = tabsState.value.tabs
        _tabsState.update {
            it.copy(
                tabs = tabs.map { tab ->
                    if (tab.id == tabId) transform(tab)
                    else tab
                }
            )
        }
    }

    fun setCurrentTab(tabId: String) {
        _tabsState.update {
            it.copy(
                currentTab = tabId
            )
        }
    }

    fun saveTabs(tabs: List<Tab>) {
        tabLocalDataSource.saveTabs(tabs)
    }

    fun loadTabs() {
        val tabs = tabLocalDataSource.loadTabs()
        // TODO: save current tab (save tabstate?)
        // TODO: save geckoview state
        _tabsState.update {
            it.copy(
                tabs = tabs,
                currentTab = tabs[0].id
            )
        }
    }
}