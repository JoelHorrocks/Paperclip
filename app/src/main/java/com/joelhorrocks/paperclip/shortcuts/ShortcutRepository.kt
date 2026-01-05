package com.joelhorrocks.paperclip.shortcuts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// TODO: combine into bookmarks?
// TODO: interface
class ShortcutRepository @Inject constructor(private val shortcutDao: ShortcutDao) {
    fun getAllShortcuts(): Flow<List<Shortcut>> = shortcutDao.getAll().map { entities -> entities.map{ Shortcut(it.id, it.url, it.name) } }

    suspend fun insertShortcuts(vararg shortcuts: Shortcut) {
        shortcutDao.insertAll(*shortcuts.map { ShortcutEntity(url = it.url, name = it.name) }.toTypedArray())
    }

    suspend fun deleteShortcut(shortcut: Shortcut) {
        shortcut.id?.let {
            shortcutDao.delete(ShortcutEntity(shortcut.id, shortcut.url, shortcut.name))
        }
        // TODO: handle null id
    }
}