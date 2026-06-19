package com.joelhorrocks.paperclip.tab

import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface TabRepository {

    val tabsState: StateFlow<TabsState>
    fun open(url: String): String
    fun insertTab(tab: Tab)
    fun close(tabId: String)
    fun update(tabId: String, transform: (Tab) -> Tab)
    fun setCurrentTab(tabId: String)
    fun setSessionSnapshot(tabId: String, snapshot: String?)
    fun saveTabs()
    fun loadTabs()

}