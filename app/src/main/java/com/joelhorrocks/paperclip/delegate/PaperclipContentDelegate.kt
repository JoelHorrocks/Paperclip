package com.joelhorrocks.paperclip.delegate

import com.joelhorrocks.paperclip.tab.TabRepository
import org.mozilla.geckoview.GeckoSession

class PaperclipContentDelegate(
    private val tabRepository: TabRepository,
    private val tabId: String,
    private val reopenSession: ((GeckoSession, String) -> Unit)
): GeckoSession.ContentDelegate {

    override fun onKill(session: GeckoSession) {
        super.onKill(session)
        if (tabId != tabRepository.tabsState.value.currentTabId) {
            return
        }

        reopenSession(
            session,
            tabRepository.tabsState.value.tabs.first { it.id == tabId }.currentUrl
        )
    }

}