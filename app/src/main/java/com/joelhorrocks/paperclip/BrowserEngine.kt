package com.joelhorrocks.paperclip

import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserEngine @Inject constructor(private val geckoRuntimeProvider: GeckoRuntimeProvider) {
    fun createSession(): GeckoSession {
        return GeckoSession().apply {
            open(geckoRuntimeProvider.runtime)
        }
    }

    fun openSession(session: GeckoSession) {
        session.open(geckoRuntimeProvider.runtime)
    }
}