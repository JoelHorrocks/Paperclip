package com.joelhorrocks.paperclip

import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

class TabControllerImpl constructor(private val browserEngine: BrowserEngine): TabController {
    private val _tabs = MutableStateFlow(listOf<Tab>())
    override val tabs = _tabs.asStateFlow()

    private val _currentTabIndex = MutableStateFlow<Int?>(null)
    override val currentTabIndex = _currentTabIndex.asStateFlow()

    private val _prompts = MutableSharedFlow<Prompt>()
    override val prompts = _prompts.asSharedFlow()

    init {
        createInitialTab()
    }

    private fun createInitialTab() {
        browserEngine.createSession().let { session ->
            _tabs.value = listOf(Tab(geckoSession = session))
            session.navigationDelegate = createNavigationDelegate()
            session.promptDelegate = createPromptDelegate()
            session.contentDelegate = createContentDelegate()
            session.loadUri(HOME_URL)
            _currentTabIndex.value = 0
        }
    }

    private fun createContentDelegate(): GeckoSession.ContentDelegate = object: GeckoSession.ContentDelegate {
        override fun onKill(session: GeckoSession) {
            super.onKill(session)
            val killedTab = _tabs.value.firstOrNull { it.geckoSession == session }
            if(killedTab == null || killedTab != _currentTabIndex.value?.let { _tabs.value[it] }) {
                return
            }

            browserEngine.openSession(session)
            session.loadUri(killedTab.currentUrl)
        }
    }

    private fun createPromptDelegate(): PromptDelegate = object: PromptDelegate {
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
                    val promptResponse = prompt.confirm(if(it) POSITIVE else NEGATIVE)
                    response.complete(promptResponse)
                })
            }
            return response
        }
    }

    private fun createNavigationDelegate(): NavigationDelegate = object: NavigationDelegate {
        // TODO: could background tab location changes incorrectly change navbar location?
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

        // TODO: consider duplication here, sometimes onLoadRequest doesn't seem to call (load about:home, then about:buildconfig, navigating back does not trigger onLoadRequest)
        // TODO: makes sure that reloading URL on same page loads full URL rather than keeping what is typed in navbar, but do we need this duplication or should we insert updated URL ourselves?
        override fun onLoadRequest(
            session: GeckoSession,
            request: NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny>? {
            _tabs.update { tabs ->
                tabs.map { tab ->
                    if (tab.geckoSession == session) tab.copy(currentUrl = request.uri)
                    else tab
                }
            }

            return super.onLoadRequest(session, request)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession?>? {
            val newSession = GeckoSession().apply {
                navigationDelegate = createNavigationDelegate()
            }
            // TODO: consolidate with createTab?
            _tabs.update {
                it + Tab(geckoSession = newSession)
            }
            _currentTabIndex.value = _tabs.value.size - 1
            // TODO: tab gets killed sometimes although below should prevent it happening?
            return GeckoResult.fromValue(newSession)
        }
    }

    override fun loadUrl(tab: Tab, url: String) {
        tab.geckoSession.loadUri(url)
    }

    override fun selectTab(index: Int) {
        // TODO: verify this is correct behaviour, should we split into own session switch function?
        // TODO: will need to notify webextension support when tab is selected when I add it
        val newSession = _tabs.value[index].geckoSession
        if(!newSession.isOpen) {
            browserEngine.openSession(newSession)
            newSession.loadUri(_tabs.value[index].currentUrl)
        }
        _currentTabIndex.value = index
    }

    override fun closeTab(index: Int) {
        if (index < 0 || index >= _tabs.value.size) return

        val sessionToClose = _tabs.value[index].geckoSession
        sessionToClose.close()

        if(_tabs.value.size == 1) {
            createInitialTab()
            _tabs.update { tabs ->
                tabs.filterIndexed { i, _ -> i != 1 }
            }
            return
        } else if(_currentTabIndex.value!! >= index && (index > 0 || _currentTabIndex.value!! > 0)) {
            _currentTabIndex.value = _currentTabIndex.value!! - 1
        }

        _tabs.update { tabs ->
            tabs.filterIndexed { i, _ -> i != index }
        }
    }

    override fun createTab() {
        browserEngine.createSession().let { session ->
            _tabs.update {
                it + Tab(geckoSession = session)
            }
            session.navigationDelegate = createNavigationDelegate()
            session.promptDelegate = createPromptDelegate()
            session.contentDelegate = createContentDelegate()
            session.loadUri(HOME_URL)
            _currentTabIndex.value = _tabs.value.size - 1
        }
    }

    override fun goBack(tab: Tab) {
        tab.geckoSession.goBack()
    }
}