package com.joelhorrocks.paperclip

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime

class GeckoRuntimeProvider (private val context: Context) {
    val runtime: GeckoRuntime by lazy {
        (context.applicationContext as BrowserApplication).geckoRuntime
    }
}