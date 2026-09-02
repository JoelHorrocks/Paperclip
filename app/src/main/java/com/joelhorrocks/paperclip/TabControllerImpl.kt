package com.joelhorrocks.paperclip

import com.joelhorrocks.paperclip.delegate.PaperclipContentDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipHistoryDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipNavigationDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipProgressDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipPromptDelegate
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
import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject

class TabControllerImpl @Inject constructor(
    private val browserEngine: BrowserEngine,
    private val tabRepository: TabRepository,
    private val historyRepository: HistoryRepository,
    private val externalScope: CoroutineScope
) : TabController {
    private val _sessions = MutableStateFlow(mapOf<String, GeckoSession>())
    override val sessions = _sessions.asStateFlow()

    private val _prompts = MutableSharedFlow<Prompt>()
    override val prompts = _prompts.asSharedFlow()

    init {
        // TODO: move to correct place
        // TODO: replace with some sort of 'ensureSession' system?
        externalScope.launch(Dispatchers.Main) {
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
                            attachDelegates(session, tab.id)
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

    private fun attachDelegates(session: GeckoSession, tabId: String) {
        session.navigationDelegate = PaperclipNavigationDelegate(
            tabRepository,
            tabId,
        ) {
            val newSession = GeckoSession()
            val tab = Tab()
            _sessions.update {
                it + Pair(tab.id, newSession)
            }
            attachDelegates(newSession, tab.id)

            tabRepository.insertTab(tab)
            tabRepository.setCurrentTab(tab.id)

            newSession
        }

        session.historyDelegate = PaperclipHistoryDelegate(historyRepository, externalScope)
        session.promptDelegate = PaperclipPromptDelegate {
            externalScope.launch {
                _prompts.emit(it)
            }
        }
        session.contentDelegate = PaperclipContentDelegate(
            tabRepository,
            tabId
        ) { session, currentUrl ->
            browserEngine.openSession(session)
            session.loadUri(currentUrl)
        }
        session.progressDelegate = PaperclipProgressDelegate(tabRepository, tabId)
    }

    override fun loadUrl(tab: Tab, url: String) {
        sessions.value[tab.id]?.loadUri(url)
    }

    override fun selectTab(tabId: String) {
        // TODO: verify this is correct behaviour, should we split into own session switch function?
        // TODO: will need to notify webextension support when tab is selected when I add it
        val newSession = sessions.value[tabId] ?: return
        // TODO: handle
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