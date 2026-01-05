package com.joelhorrocks.paperclip

import com.joelhorrocks.paperclip.history.HistoryEntry
import com.joelhorrocks.paperclip.history.HistoryRepository
import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.tab.TabRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.HistoryDelegate
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE

class TabControllerImpl(
    private val browserEngine: BrowserEngine,
    private val tabRepository: TabRepository,
    private val historyRepository: HistoryRepository
) : TabController {
    private val _sessions = MutableStateFlow(mapOf<String, GeckoSession>())
    override val sessions = _sessions.asStateFlow()

    private val _prompts = MutableSharedFlow<Prompt>()
    override val prompts = _prompts.asSharedFlow()

    init {
        // TODO: move to correct place
        // TODO: replace with some sort of 'ensureSession' system?
        CoroutineScope(Dispatchers.Main).launch {
            tabRepository.tabsState
                .map { state -> state.tabs.map { it.id }.toSet() }
                .distinctUntilChanged()
                .collect { tabIds ->
                    val toCreate = tabIds - sessions.value.keys
                    val toClose = sessions.value.keys - tabIds
                    // TODO: batch updates?
                    for (id in toCreate) {
                        val tab = tabRepository.tabsState.value.tabs.first { it.id == id }
                        browserEngine.createSession().let { session ->
                            _sessions.update {
                                it + Pair(tab.id, session)
                            }
                            session.navigationDelegate = createNavigationDelegate()
                            session.historyDelegate = createHistoryDelegate()
                            session.promptDelegate = createPromptDelegate()
                            session.contentDelegate = createContentDelegate()
                            session.progressDelegate = createProgressDelegate()
                            GeckoSession.SessionState.fromString(tab.sessionSnapshot)?.let {
                                // TODO: some way to control session snapshots to ensure it matches with engine
                                session.restoreState(it)
                            } ?: session.loadUri(tab.currentUrl)
                        }
                    }
                    for (id in toClose) {
                        val session = sessions.value[id]
                        session?.close()
                        _sessions.update {
                            it.filter { comparisonSession -> comparisonSession.key != id }
                        }
                    }
                }
        }
    }

    private fun createProgressDelegate(): GeckoSession.ProgressDelegate =
        object : GeckoSession.ProgressDelegate {
            // TODO: find a way to avoid having to store this in the tab object - it's not required anywhere in the UI so no point holding it there
            // TODO: we could immediately write this to disk in tabRepository and bypass tab entirely?
            override fun onSessionStateChange(p0: GeckoSession, p1: GeckoSession.SessionState) {
                super.onSessionStateChange(p0, p1)
                tabRepository.setSessionSnapshot(tabRepository.tabsState.value.tabs.first { sessions.value[it.id] == p0 }.id, p1.toString())
            }
        }

    private fun createContentDelegate(): GeckoSession.ContentDelegate =
        object : GeckoSession.ContentDelegate {
            override fun onKill(session: GeckoSession) {
                super.onKill(session)
                val killedTab =
                    tabRepository.tabsState.value.tabs.firstOrNull { sessions.value[it.id] == session }
                if (killedTab == null || killedTab != tabRepository.tabsState.value.tabs.first { it.id == tabRepository.tabsState.value.currentTab }) {
                    return
                }

                browserEngine.openSession(session)
                session.loadUri(killedTab.currentUrl)
            }
        }

    private fun createPromptDelegate(): PromptDelegate = object : PromptDelegate {
        override fun onAlertPrompt(
            session: GeckoSession,
            prompt: PromptDelegate.AlertPrompt
        ): GeckoResult<PromptDelegate.PromptResponse?>? {
            CoroutineScope(Dispatchers.Main).launch {
                _prompts.emit(Prompt.Alert(prompt.title, prompt.message))
            }
            return super.onAlertPrompt(session, prompt)
        }

        override fun onButtonPrompt(
            session: GeckoSession,
            prompt: PromptDelegate.ButtonPrompt
        ): GeckoResult<PromptDelegate.PromptResponse?>? {
            val response = GeckoResult<PromptDelegate.PromptResponse?>()
            CoroutineScope(Dispatchers.Main).launch {
                _prompts.emit(Prompt.Button(prompt.title, prompt.message) {
                    val promptResponse = prompt.confirm(if (it) POSITIVE else NEGATIVE)
                    response.complete(promptResponse)
                })
            }
            return response
        }
    }

    private fun createHistoryDelegate(): HistoryDelegate = object : HistoryDelegate {
        override fun onVisited(
            session: GeckoSession,
            url: String,
            lastVisitedURL: String?,
            flags: Int
        ): GeckoResult<Boolean?>? {
            // TODO: store visit type then filter in UI?
            if (
                // skip about URLs
                url.startsWith("about:") ||
                // skip redirects
                flags and HistoryDelegate.VISIT_REDIRECT_SOURCE_PERMANENT != 0 ||
                flags and HistoryDelegate.VISIT_REDIRECT_SOURCE != 0 ||
                // skip iframe navigations
                flags and HistoryDelegate.VISIT_TOP_LEVEL == 0 ||
                // skip errors
                flags and HistoryDelegate.VISIT_UNRECOVERABLE_ERROR != 0 ||
                // skip reloads
                lastVisitedURL?.let { it == url } ?: false
                    ) return GeckoResult.fromValue(false)

            CoroutineScope(Dispatchers.IO).launch {
                historyRepository.insertHistoryEntries(
                    HistoryEntry(
                        id = null,
                        url = url,
                        title = "", // TODO: get title
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            return super.onVisited(session, url, lastVisitedURL, flags)
        }
    }

    private fun createNavigationDelegate(): NavigationDelegate = object : NavigationDelegate {
        // TODO: could background tab location changes incorrectly change navbar location?
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            super.onLocationChange(session, url, perms, hasUserGesture)

            tabRepository.update(sessions.value.filter { it.value == session }.keys.first()) {
                it.copy(currentUrl = url ?: "")
            }
        }

        // TODO: consider duplication here, sometimes onLoadRequest doesn't seem to call (load about:home, then about:buildconfig, navigating back does not trigger onLoadRequest)
        // TODO: makes sure that reloading URL on same page loads full URL rather than keeping what is typed in navbar, but do we need this duplication or should we insert updated URL ourselves?
        override fun onLoadRequest(
            session: GeckoSession,
            request: NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny>? {
            tabRepository.update(sessions.value.filter { it.value == session }.keys.first()) {
                it.copy(currentUrl = request.uri)
            }

            return super.onLoadRequest(session, request)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession?>? {
            // TODO: fix!!! - use some sort of ensureSession??
            val newSession = GeckoSession().apply {
                // TODO: add additional delegates
                navigationDelegate = createNavigationDelegate()
                historyDelegate = createHistoryDelegate()
                promptDelegate = createPromptDelegate()
                contentDelegate = createContentDelegate()
                progressDelegate = createProgressDelegate()
            }
            val tab = Tab()
            _sessions.update {
                it + Pair(tab.id, newSession)
            }
            tabRepository.insertTab(tab)
            tabRepository.setCurrentTab(tab.id)

            return GeckoResult.fromValue(newSession)
        }
    }

    override fun loadUrl(tab: Tab, url: String) {
        sessions.value[tab.id]?.loadUri(url)
    }

    override fun selectTab(tabId: String) {
        // TODO: verify this is correct behaviour, should we split into own session switch function?
        // TODO: will need to notify webextension support when tab is selected when I add it
        val newSession = sessions.value[tabId]
        // TODO: handle
        if (newSession == null) return
        if (!newSession.isOpen) {
            browserEngine.openSession(newSession)
            newSession.loadUri(tabRepository.tabsState.value.tabs.first { it.id == tabId }.currentUrl)
        }
        tabRepository.setCurrentTab(tabId)
    }

    override fun closeTab(tabId: String) {
        val sessionToClose = sessions.value[tabId]
        sessionToClose?.close()

        tabRepository.close(tabId)
    }

    override fun createTab() {
        tabRepository.open(HOME_URL)
    }

    override fun goBack(tab: Tab) {
        sessions.value[tab.id]?.goBack()
    }
}