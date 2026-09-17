package com.joelhorrocks.paperclip.tab

import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.model.Tab
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

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

    @Test
    fun `updating tab with transform changes tab`() {
        val firstTab = tabRepository.currentTab
        val secondTab = Tab()

        tabRepository.insertTab(secondTab)

        tabRepository.update(firstTab.id) {
            it.copy(
                currentUrl = "new url"
            )
        }

        assertEquals(
            "Tab should change when updated",
            "new url",
            tabRepository.tabs.first { it.id == firstTab.id }.currentUrl
        )

        assertEquals(
            "Other tabs should not be affected by update",
            secondTab,
            tabRepository.tabs.first { it.id == secondTab.id }
        )
    }

    @Test
    fun `updating tab that does not exist makes no changes`() {
        val tabs = tabRepository.tabs

        tabRepository.update(UUID.randomUUID().toString()) {
            error("Transform should not be called for invalid tab id")
        }

        assertEquals(
            "No changes should be made when tab does not exist",
            tabs,
            tabRepository.tabs
        )
    }

    @Test
    fun `loading tabs with none saved opens a homepage tab`() {
        tabRepository.insertTab(Tab(currentUrl = "some url"))

        tabRepository.loadTabs()

        assertEquals(
            "Loading tabs with none saved should open one tab",
            1,
            tabRepository.tabs.size
        )

        assertEquals(
            "Loading tabs with none saved should open a homepage tab",
            HOME_URL,
            tabRepository.currentTab.currentUrl
        )
    }

    @Test
    fun `loading tabs reverts to saved tabs`() {
        tabRepository.insertTab(Tab())
        val tabsList = tabRepository.tabs

        tabRepository.saveTabs()
        tabRepository.insertTab(Tab())
        tabRepository.loadTabs()

        assertEquals(
            "Loading tabs should revert to saved tabs",
            tabsList,
            tabRepository.tabs
        )
    }
}

private val TabRepository.tabs get() = tabsState.value.tabs
private val TabRepository.currentTab get() = tabsState.value.currentTab!!