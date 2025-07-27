package com.joelhorrocks.paperclip.tab

import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// TODO: combine with in-memory tab/session storage and move that out of TabController?
// TODO: interface
class TabRepository() {
    private val _tabs = MutableStateFlow(listOf<Tab>())
    val tabs = _tabs.asStateFlow()

    private val _currentTab = MutableStateFlow("")
    val currentTab = _currentTab.asStateFlow()

    init {
        initialize()
    }

    // Load from JSON if present, otherwise create file and initial tab
    private fun initialize() {
        open(HOME_URL)
    }

    fun open(url: String): String {
        val newTab = Tab(
            currentUrl = url
        )
        _tabs.update {
            it + newTab
        }
        // TODO: handle here?
        _currentTab.update {
            newTab.id
        }
        return newTab.id
    }

    // TODO: create tabs here or manage with just URL?
    fun insertTab(tab: Tab) {
        _tabs.update {
            it + tab
        }
    }

    fun close(tabId: String) {
        val index = tabs.value.indexOfFirst { it.id == tabId }
        val currentTabIndex = tabs.value.indexOfFirst { it.id == currentTab.value }

        // TODO: clean up now we're using IDs instead of indexes
        if(_tabs.value.size == 1) {
            // TODO: replace when initialize handles JSON?
            initialize()
            _tabs.update { tabs ->
                tabs.filterIndexed { i, _ -> i != 1 }
            }
            return
        } else if(currentTabIndex == index && index == 0) {
            _currentTab.value = tabs.value[tabs.value.indexOfFirst { it.id == currentTab.value } + 1].id
        } else if(currentTabIndex == index) {
            _currentTab.value = tabs.value[tabs.value.indexOfFirst { it.id == currentTab.value } - 1].id
        }

        _tabs.update {
            tabs.value.filter { it.id != tabId }
        }
    }

    fun update(tabId: String, transform: (Tab) -> Tab) {
        _tabs.update {
            tabs.value.map { tab ->
                if(tab.id == tabId) transform(tab)
                else tab
            }
        }
    }

    fun setCurrentTab(tabId: String) {
        _currentTab.value = tabId
    }
}