package com.joelhorrocks.paperclip

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

@Singleton
class TabController @Inject constructor(private val browserEngine: BrowserEngine) {
    data class Tab(
        val id: String = UUID.randomUUID().toString(),
        val geckoSession: GeckoSession,
        val isLoading: Boolean = false,
        val currentUrl: String = ""
    )

    sealed class Prompt {
        class Alert(val title: String?, val message: String?) : Prompt()
        // TODO: CompletableDeferred instead of callback?
        class Button(val title: String?, val message: String?, val onAction: (confirm: Boolean) -> Unit) : Prompt()
    }

    private val _tabs = MutableStateFlow(listOf<Tab>())
    val tabs = _tabs.asStateFlow()

    private val _currentTabIndex = MutableStateFlow<Int?>(null)
    val currentTabIndex = _currentTabIndex.asStateFlow()

    private val _prompts = MutableSharedFlow<Prompt>()
    val prompts = _prompts.asSharedFlow()

    init {
        createInitialTab()
    }

    private fun createInitialTab() {
        browserEngine.createSession().let { session ->
            _tabs.value = listOf(Tab(geckoSession = session))
            session.navigationDelegate = createNavigationDelegate()
            session.promptDelegate = createPromptDelegate()
            session.loadUri(HOME_URL)
            _currentTabIndex.value = 0
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
            return GeckoResult.fromValue(newSession)
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
        } else if(_currentTabIndex.value!! >= index && (index > 0 || _currentTabIndex.value!! > 0)) {
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
            session.loadUri(HOME_URL)
            _currentTabIndex.value = _tabs.value.size - 1
        }
    }

    fun goBack(tab: Tab) {
        tab.geckoSession.goBack()
    }
}