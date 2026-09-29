package com.joelhorrocks.paperclip

import android.util.Log
import com.joelhorrocks.paperclip.delegate.PaperclipContentDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipHistoryDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipNavigationDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipProgressDelegate
import com.joelhorrocks.paperclip.delegate.PaperclipPromptDelegate
import com.joelhorrocks.paperclip.history.HistoryRepository
import com.joelhorrocks.paperclip.ml.TranslationModel
import com.joelhorrocks.paperclip.ml.Translator
import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.tab.TabRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import kotlin.collections.plus
import kotlin.collections.set

// TODO: interface, GeckoLiveTab?
class LiveTab @AssistedInject constructor(
    @Assisted val geckoSession: GeckoSession,
    @Assisted val tabId: String,
    private val historyRepository: HistoryRepository,
    private val tabRepository: TabRepository,
    private val browserEngine: BrowserEngine,
    private val translator: Translator,
    // TODO: tab lifetime scope?
    private val externalScope: CoroutineScope
) {
    private val _prompts = MutableSharedFlow<Prompt>()
    val prompts = _prompts.asSharedFlow()

    private val _sessions = MutableSharedFlow<Pair<GeckoSession, String>>()
    val sessions = _sessions.asSharedFlow()

    val isOpen
        get() = geckoSession.isOpen

    private var port: WebExtension.Port? = null

    init {
        geckoSession.historyDelegate = PaperclipHistoryDelegate(historyRepository, externalScope)
        geckoSession.progressDelegate = PaperclipProgressDelegate(tabRepository, tabId)
        geckoSession.promptDelegate = PaperclipPromptDelegate {
            externalScope.launch {
                _prompts.emit(it)
            }
        }
        geckoSession.navigationDelegate = PaperclipNavigationDelegate(
            tabRepository,
            tabId,
        ) {
            val newSession = GeckoSession()
            val tab = Tab()

            externalScope.launch {
                _sessions.emit(Pair(newSession, tab.id))
            }

            tabRepository.insertTab(tab)
            tabRepository.setCurrentTab(tab.id)

            newSession
        }
        geckoSession.contentDelegate = PaperclipContentDelegate(
            tabRepository,
            tabId
        ) { session, currentUrl ->
            browserEngine.openSession(session)
            session.loadUri(currentUrl)
        }

        val paperclipTranslateDelegate = object : WebExtension.MessageDelegate {
            override fun onConnect(p0: WebExtension.Port) {
                port = p0

                p0.setDelegate(
                    object : WebExtension.PortDelegate {
                        override fun onPortMessage(p0: Any, p1: WebExtension.Port) {
                            val json = Json.parseToJsonElement(p0.toString())
                            Log.d("extensions", json.toString())
                            // TODO: get translator from vm
                            externalScope.launch {
                                val result = json.jsonObject["result"]!!.jsonObject
                                val modelId = json.jsonObject["model_id"]!!.jsonPrimitive.content
                                for(i in result.keys) {
                                    // TODO: tab scope?
                                    // TODO: handle too long text
                                    // TODO: better sentence splitting
                                    val text = result[i]!!.jsonPrimitive.content.split(".")

                                    val out = mutableListOf<String>()

                                    for(t in text) {
                                        val translation = translator.translate(modelId, t)
                                        out.add(translation)
                                    }

                                    port?.postMessage(
                                        JSONObject().apply {
                                            put("type", "result")
                                            put("id", i)
                                            put("translation", out.joinToString("."))
                                        }
                                    )
                                }
                            }
                            super.onPortMessage(p0, p1)
                        }
                    }
                )
                Log.d("Extensions", "PORT CONNECT")
                super.onConnect(p0)
            }
        }
        browserEngine.setTranslateDelegate(geckoSession, paperclipTranslateDelegate)
    }

    fun close() {
        geckoSession.close()
    }

    fun loadUri(uri: String) {
        geckoSession.loadUri(uri)
    }

    fun goBack() {
        geckoSession.goBack()
    }

    fun translate(model: TranslationModel) {
        port?.postMessage(
            JSONObject().apply {
                put("type", "translate")
                put("model_id", model.id)
            }
        )
    }

    @AssistedFactory
    interface Factory { fun create(@Assisted geckoSession: GeckoSession, @Assisted tabId: String): LiveTab }
}