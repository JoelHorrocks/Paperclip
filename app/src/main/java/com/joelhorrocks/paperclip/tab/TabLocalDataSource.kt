package com.joelhorrocks.paperclip.tab

import android.content.Context
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.serialization.json.Json

class TabLocalDataSource(private val context: Context) {
    fun loadTabs(): List<Tab> {
        val tabsJson = context.openFileInput("tabs.json").bufferedReader().readText()
        val tabs = Json.decodeFromString<List<Tab>>(tabsJson)
        return tabs
    }

    fun saveTabs(tabs: List<Tab>) {
        val tabs = Json.encodeToString(tabs)
        context.openFileOutput("tabs.json", Context.MODE_PRIVATE).use {
            it.write(tabs.toByteArray())
        }
    }
}