package com.joelhorrocks.paperclip

import com.joelhorrocks.paperclip.ml.TranslationModel
import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.GeckoSession

interface TabController {
    val sessions: StateFlow<Map<String, LiveTab>>
    val prompts: SharedFlow<Prompt>

    fun loadUrl(tab: Tab, url: String)
    fun selectTab(tabId: String)
    fun closeTab(tabId: String)
    fun createTab()
    fun goBack(tab: Tab)
    fun sendTranslateRequest(tabId: String, model: TranslationModel)
}