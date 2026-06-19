package com.joelhorrocks.paperclip.tab

import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.model.Tab
import org.junit.Assert.assertEquals
import org.junit.Test

class TabRepositoryTest {

    private val dataSource = FakeTabLocalDataSource()
    private val tabRepository = TabRepositoryImpl(dataSource)

    @Test
    fun `closing last tab opens a new home tab`() {
        tabRepository.close(tabRepository.currentTab.id)

        assertEquals(
            "New tab should be opened when last tab is closed",
            1,
            tabRepository.tabs.size
        )

        assertEquals(
            "New tab opened when last tab is closed should be homepage tab",
            HOME_URL,
            tabRepository.currentTab.currentUrl
        )
    }

    @Test
    fun `closing non-current tab does not change current tab`() {
        val firstTab = tabRepository.currentTab
        val newTab = Tab()
        tabRepository.insertTab(newTab)

        tabRepository.setCurrentTab(newTab.id)
        tabRepository.close(firstTab.id)

        assertEquals(
            "Current tab should stay selected when non-current tab is closed",
            newTab,
            tabRepository.currentTab
        )
    }

    @Test
    fun `closing current tab with previous available opens previous tab`() {
        val firstTab = tabRepository.currentTab
        val secondTab = Tab()
        val thirdTab = Tab()

        tabRepository.insertTab(secondTab)
        tabRepository.insertTab(thirdTab)

        tabRepository.setCurrentTab(secondTab.id)
        tabRepository.close(secondTab.id)

        assertEquals(
            "Previous tab should be selected when current tab is closed",
            firstTab,
            tabRepository.currentTab
        )
    }

    @Test
    fun `closing current tab with no previous available opens next tab`() {
        val firstTab = tabRepository.currentTab
        val secondTab = Tab()

        tabRepository.insertTab(secondTab)

        tabRepository.setCurrentTab(firstTab.id)
        tabRepository.close(firstTab.id)

        assertEquals(
            "Next tab should be selected when current tab is closed",
            secondTab,
            tabRepository.currentTab
        )
    }
}

private val TabRepository.tabs get() = tabsState.value.tabs
// TODO: do a proper currentTab for TabRepository in general (we use this a lot)
private val TabRepository.currentTab get() = tabs.first { it.id == tabsState.value.currentTab }