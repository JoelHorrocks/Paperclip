package com.joelhorrocks.paperclip.delegate

import com.joelhorrocks.paperclip.model.Prompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE

class PaperclipPromptDelegate(
    private val emitPrompt: ((Prompt) -> Unit)
): GeckoSession.PromptDelegate {
    override fun onAlertPrompt(
        session: GeckoSession,
        prompt: PromptDelegate.AlertPrompt
    ): GeckoResult<PromptDelegate.PromptResponse?>? {
        emitPrompt(Prompt.Alert(prompt.title, prompt.message))
        return super.onAlertPrompt(session, prompt)
    }

    override fun onButtonPrompt(
        session: GeckoSession,
        prompt: PromptDelegate.ButtonPrompt
    ): GeckoResult<PromptDelegate.PromptResponse?>? {
        val response = GeckoResult<PromptDelegate.PromptResponse?>()
        emitPrompt(
            Prompt.Button(prompt.title, prompt.message) {
            val promptResponse = prompt.confirm(if (it) POSITIVE else NEGATIVE)
            response.complete(promptResponse)
        })
        return response
    }
}