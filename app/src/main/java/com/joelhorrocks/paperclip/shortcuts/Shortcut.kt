package com.joelhorrocks.paperclip.shortcuts

data class Shortcut(
    // TODO: make this nullable or separate db and shorcut ID and use a UUID?
    val id: Int? = null,
    val url: String,
    val name: String,
)
