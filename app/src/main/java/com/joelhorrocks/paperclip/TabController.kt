package com.joelhorrocks.paperclip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabController @Inject constructor(private val browserEngine: BrowserEngine) {
    data class Tab(
        val id: String = UUID.randomUUID().toString(),
        val geckoSession: GeckoSession,
        val isLoading: Boolean = false,
        val currentUrl: String = ""
    )

    private val _tabs = MutableStateFlow(listOf<Tab>())
    val tabs = _tabs.asStateFlow()

    private val _currentTabIndex = MutableStateFlow<Int?>(null)
    val currentTabIndex = _currentTabIndex.asStateFlow()

    init {
        createInitialTab()
    }

    private fun createInitialTab() {
        browserEngine.createSession().let { session ->
            _tabs.value = listOf(Tab(geckoSession = session))
            session.navigationDelegate = createNavigationDelegate()
            //session.loadUri("")
            _currentTabIndex.value = 0
        }
    }

    private fun createNavigationDelegate() = object : NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            super.onLocationChange(session, url, perms, hasUserGesture)

            _tabs.update { tabs ->
                tabs.map { tab ->
                    if (tab.geckoSession == session) tab.copy(currentUrl = url ?: "")
                    else tab
                }
            }
        }
    }

    fun loadUrl(tab: Tab, url: String) {
        tab.geckoSession.loadUri(url)
    }

    fun selectTab(index: Int) {
        _currentTabIndex.value = index
    }

    fun closeTab(index: Int) {
        if (index < 0 || index >= _tabs.value.size) return

        val sessionToClose = _tabs.value[index].geckoSession
        sessionToClose.close()

        if(_tabs.value.size == 1) {
            createInitialTab()
            _tabs.update { tabs ->
                tabs.filterIndexed { i, _ -> i != 1 }
            }
            return
        } else if(_currentTabIndex.value!! >= index) {
            _currentTabIndex.value = _currentTabIndex.value!! - 1
        }

        _tabs.update { tabs ->
            tabs.filterIndexed { i, _ -> i != index }
        }
    }

    fun createTab() {
        browserEngine.createSession().let { session ->
            _tabs.update {
                it + Tab(geckoSession = session)
            }
            session.navigationDelegate = createNavigationDelegate()
            //session.loadUri("about:buildconfig")
            _currentTabIndex.value = _tabs.value.size - 1
        }
    }

    fun goBack(tab: Tab) {
        tab.geckoSession.goBack()
    }
}