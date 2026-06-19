package com.joelhorrocks.paperclip.tab

import com.joelhorrocks.paperclip.model.Tab

class FakeTabLocalDataSource: TabLocalDataSource {

    private var tabsList: List<Tab> = emptyList()

    override fun loadTabs(): List<Tab> {
        return tabsList
    }

    override fun saveTabs(tabs: List<Tab>) {
        tabsList = tabs.toList()
    }
}