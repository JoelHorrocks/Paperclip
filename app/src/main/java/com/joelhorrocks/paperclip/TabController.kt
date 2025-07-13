package com.joelhorrocks.paperclip

import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TabController {
    val tabs: StateFlow<List<Tab>>
    val currentTabIndex: StateFlow<Int?>
    val prompts: SharedFlow<Prompt>

    fun loadUrl(tab: Tab, url: String)
    fun selectTab(index: Int)
    fun closeTab(index: Int)
    fun createTab()
    fun goBack(tab: Tab)
}