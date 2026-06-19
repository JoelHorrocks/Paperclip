package com.joelhorrocks.paperclip.tab

import com.joelhorrocks.paperclip.HOME_URL
import org.junit.Assert.assertEquals
import org.junit.Test

class TabRepositoryTest {

    private val dataSource = FakeTabLocalDataSource()
    private val tabRepository = TabRepositoryImpl(dataSource)

    @Test
    fun `closing last tab opens a new home tab`() {
        assertEquals(
            "TabRepository should initialize with one tab",
            1,
            tabRepository.tabsState.value.tabs.size
        )

        tabRepository.close(tabRepository.tabsState.value.tabs.first().id)

        assertEquals(
            "New tab should be opened when last tab is closed",
            1,
            tabRepository.tabsState.value.tabs.size
        )

        assertEquals(
            "New tab opened when last tab is closed should be homepage tab",
            HOME_URL,
            tabRepository.tabsState.value.tabs.first().currentUrl
        )
    }
}