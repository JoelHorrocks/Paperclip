package com.joelhorrocks.paperclip

import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserEngine @Inject constructor(private val geckoRuntime: GeckoRuntime) {
    fun createSession(): GeckoSession {
        return GeckoSession().apply {
            open(geckoRuntime)
        }
    }

    fun openSession(session: GeckoSession) {
        session.open(geckoRuntime)
    }
}