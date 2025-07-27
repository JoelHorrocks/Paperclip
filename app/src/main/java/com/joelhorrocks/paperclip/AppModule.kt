package com.joelhorrocks.paperclip

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import com.joelhorrocks.paperclip.news.NewsRepository
import com.joelhorrocks.paperclip.settings.SettingsRepository
import com.joelhorrocks.paperclip.settings.SettingsRepositoryImpl
import com.joelhorrocks.paperclip.shortcuts.ShortcutDao
import com.joelhorrocks.paperclip.shortcuts.ShortcutRepository
import com.joelhorrocks.paperclip.tab.TabRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideGeckoRuntimeProvider(@ApplicationContext appContext: Context): GeckoRuntimeProvider {
        return GeckoRuntimeProvider(appContext)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            produceFile = { appContext.dataStoreFile("user_prefs.preferences_pb") }
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java, "paperclip-database"
        ).build()
    }

    @Provides
    fun provideShortcutDao(appDatabase: AppDatabase): ShortcutDao {
        return appDatabase.shortcutDao()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideNewsRepository(): NewsRepository {
        return NewsRepository()
    }

    @Provides
    @Singleton
    fun provideTabRepository(): TabRepository {
        return TabRepository()
    }

    @Provides
    @Singleton
    fun provideShortcutsRepository(shortcutDao: ShortcutDao): ShortcutRepository {
        return ShortcutRepository(shortcutDao)
    }

    @Provides
    @Singleton
    fun provideTabController(browserEngine: BrowserEngine, tabRepository: TabRepository): TabController {
        return TabControllerImpl(browserEngine, tabRepository)
    }
}