package com.joelhorrocks.paperclip.delegate

import com.joelhorrocks.paperclip.history.HistoryEntry
import com.joelhorrocks.paperclip.history.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.HistoryDelegate

class PaperclipHistoryDelegate(
    private val historyRepository: HistoryRepository,
    private val externalScope: CoroutineScope
): HistoryDelegate {

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

        externalScope.launch {
            withContext(Dispatchers.IO) {
                historyRepository.insertHistoryEntries(
                    HistoryEntry(
                        id = null,
                        url = url,
                        title = "", // TODO: get title
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        return super.onVisited(session, url, lastVisitedURL, flags)
    }

}