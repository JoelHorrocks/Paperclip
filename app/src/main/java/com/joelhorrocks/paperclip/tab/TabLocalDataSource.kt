package com.joelhorrocks.paperclip.tab

import android.content.Context
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.serialization.json.Json

interface TabLocalDataSource {
    fun loadTabs(): List<Tab>
    fun saveTabs(tabs: List<Tab>)
}