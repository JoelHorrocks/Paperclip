package com.joelhorrocks.paperclip.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    // TODO: should this be in settings or an app data repository?
    val showDrawerTooltip: Flow<Boolean>
    suspend fun setShowDrawerTooltip(value: Boolean)
}