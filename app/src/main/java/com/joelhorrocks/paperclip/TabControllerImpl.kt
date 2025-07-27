package com.joelhorrocks.paperclip

import android.util.Log
import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.tab.TabRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class TabControllerImpl(
    private val browserEngine: BrowserEngine,
    private val tabRepository: TabRepository
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
                            session.promptDelegate = createPromptDelegate()
                            session.contentDelegate = createContentDelegate()
                            session.loadUri(tab.currentUrl)
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