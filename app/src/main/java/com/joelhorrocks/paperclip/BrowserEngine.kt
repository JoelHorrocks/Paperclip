package com.joelhorrocks.paperclip

import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
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

    // TODO: error handling
    // TODO: a bit messy, decide best place to put this
    fun setTranslateDelegate(session: GeckoSession, messageDelegate: WebExtension.MessageDelegate) {
        geckoRuntime.webExtensionController
            .ensureBuiltIn("resource://android/assets/translate/", "translate@joelhorrocks.com")
            .accept( // Set delegate that will receive messages coming from this extension.
                { extension ->
                    session.webExtensionController
                        .setMessageDelegate(extension!!, messageDelegate, "browser")
                },  // Something bad happened, let's log an error
                { e -> Log.e("MessageDelegate", "Error registering extension", e) }
            )
    }
}