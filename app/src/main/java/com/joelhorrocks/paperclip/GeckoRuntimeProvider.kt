package com.joelhorrocks.paperclip

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mozilla.geckoview.GeckoRuntime
import javax.inject.Inject

class GeckoRuntimeProvider (private val context: Context) {
    val runtime: GeckoRuntime by lazy {
        (context.applicationContext as BrowserApplication).geckoRuntime
    }
}