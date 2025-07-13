package com.joelhorrocks.paperclip.model

sealed class Prompt {
    class Alert(val title: String?, val message: String?) : Prompt()
    // TODO: CompletableDeferred instead of callback?
    class Button(val title: String?, val message: String?, val onAction: (confirm: Boolean) -> Unit) : Prompt()
}