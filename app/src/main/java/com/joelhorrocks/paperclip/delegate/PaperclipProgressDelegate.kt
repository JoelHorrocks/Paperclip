package com.joelhorrocks.paperclip.delegate

import com.joelhorrocks.paperclip.tab.TabRepository
import org.mozilla.geckoview.GeckoSession

class PaperclipProgressDelegate(
    private val tabRepository: TabRepository,
    private val tabId: String
): GeckoSession.ProgressDelegate {
    // TODO: find a way to avoid having to store this in the tab object - it's not required anywhere in the UI so no point holding it there
    // TODO: we could immediately write this to disk in tabRepository and bypass tab entirely?
    override fun onSessionStateChange(p0: GeckoSession, p1: GeckoSession.SessionState) {
        super.onSessionStateChange(p0, p1)
        tabRepository.setSessionSnapshot(tabId, p1.toString())
    }
}