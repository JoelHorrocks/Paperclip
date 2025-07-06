package com.joelhorrocks.paperclip.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
): SettingsRepository {
    private object Keys {
        val SHOW_DRAWER_TOOLTIP = booleanPreferencesKey("show_drawer_tooltip")
    }

    override val showDrawerTooltip: Flow<Boolean>
        get() = dataStore.data
            .map { prefs -> prefs[Keys.SHOW_DRAWER_TOOLTIP] ?: true }
            .catch { emit(true) }

    override suspend fun setShowDrawerTooltip(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_DRAWER_TOOLTIP] = value }
    }
}