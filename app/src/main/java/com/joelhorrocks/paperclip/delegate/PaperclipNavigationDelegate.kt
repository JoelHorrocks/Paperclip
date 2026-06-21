package com.joelhorrocks.paperclip.delegate

import com.joelhorrocks.paperclip.tab.TabRepository
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate

class PaperclipNavigationDelegate(
    private val tabRepository: TabRepository,
    private val tabId: String,
    private val createNewSession: (() -> GeckoSession)
): NavigationDelegate {
    private var pastInitialLoad = false

    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean
    ) {
        super.onLocationChange(session, url, perms, hasUserGesture)

        // GeckoView loads about:blank on session creation before we can set our desired URL
        // See https://github.com/mozilla-mobile/android-components/issues/403
        if(pastInitialLoad) {
            tabRepository.update(tabId) {
                it.copy(currentUrl = url ?: "")
            }
        } else {
            pastInitialLoad = true
        }
    }

    // TODO: consider duplication here, sometimes onLoadRequest doesn't seem to call (load about:home, then about:buildconfig, navigating back does not trigger onLoadRequest)
    // TODO: makes sure that reloading URL on same page loads full URL rather than keeping what is typed in navbar, but do we need this duplication or should we insert updated URL ourselves?
    override fun onLoadRequest(
        session: GeckoSession,
        request: NavigationDelegate.LoadRequest
    ): GeckoResult<AllowOrDeny>? {
        tabRepository.update(tabId) {
            it.copy(currentUrl = request.uri)
        }

        return super.onLoadRequest(session, request)
    }

    override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession?> {
        val newSession = createNewSession()
        return GeckoResult.fromValue(newSession)
    }
}