package com.joelhorrocks.paperclip

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import com.joelhorrocks.paperclip.history.HistoryDao
import com.joelhorrocks.paperclip.history.HistoryRepository
import com.joelhorrocks.paperclip.history.HistoryRepositoryImpl
import com.joelhorrocks.paperclip.ml.remote.TranslationModelRemoteDataSource
import com.joelhorrocks.paperclip.ml.TranslationModelRepository
import com.joelhorrocks.paperclip.ml.TranslationModelRepositoryImpl
import com.joelhorrocks.paperclip.ml.local.TranslationModelDao
import com.joelhorrocks.paperclip.ml.local.TranslationModelLocalDataSource
import com.joelhorrocks.paperclip.news.NewsRepository
import com.joelhorrocks.paperclip.news.NewsRepositoryImpl
import com.joelhorrocks.paperclip.settings.SettingsRepository
import com.joelhorrocks.paperclip.settings.SettingsRepositoryImpl
import com.joelhorrocks.paperclip.shortcuts.ShortcutDao
import com.joelhorrocks.paperclip.shortcuts.ShortcutRepository
import com.joelhorrocks.paperclip.shortcuts.ShortcutRepositoryImpl
import com.joelhorrocks.paperclip.tab.FileTabLocalDataSource
import com.joelhorrocks.paperclip.tab.TabLocalDataSource
import com.joelhorrocks.paperclip.tab.TabRepository
import com.joelhorrocks.paperclip.tab.TabRepositoryImpl
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
    fun provideHttpClientProvider(): HttpClientProvider {
        return HttpClientProvider()
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
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }

    @Provides
    fun provideTranslationModelDao(appDatabase: AppDatabase): TranslationModelDao {
        return appDatabase.translationModelDao()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideTranslationModelRepository(translationModelLocalDataSource: TranslationModelLocalDataSource, translationModelRemoteDataSource: TranslationModelRemoteDataSource): TranslationModelRepository {
        return TranslationModelRepositoryImpl(translationModelLocalDataSource, translationModelRemoteDataSource)
    }

    @Provides
    @Singleton
    fun provideTranslationModelRemoteDataSource(): TranslationModelRemoteDataSource {
        return TranslationModelRemoteDataSource()
    }

    @Provides
    @Singleton
    fun provideTranslationModelLocalDataSource(translationModelDao: TranslationModelDao): TranslationModelLocalDataSource {
        return TranslationModelLocalDataSource(translationModelDao)
    }

    @Provides
    @Singleton
    fun provideNewsRepository(httpClientProvider: HttpClientProvider): NewsRepository {
        return NewsRepositoryImpl(httpClientProvider)
    }

    @Provides
    @Singleton
    fun provideTabLocalDataSource(@ApplicationContext appContext: Context): TabLocalDataSource {
        return FileTabLocalDataSource(appContext)
    }

    @Provides
    @Singleton
    fun provideTabRepository(tabLocalDataSource: TabLocalDataSource): TabRepository {
        return TabRepositoryImpl(tabLocalDataSource)
    }

    @Provides
    @Singleton
    fun provideShortcutsRepository(shortcutDao: ShortcutDao): ShortcutRepository {
        return ShortcutRepositoryImpl(shortcutDao)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository {
        return HistoryRepositoryImpl(historyDao)
    }

    @Provides
    @Singleton
    fun provideTabController(browserEngine: BrowserEngine, tabRepository: TabRepository, historyRepository: HistoryRepository): TabController {
        return TabControllerImpl(browserEngine, tabRepository, historyRepository)
    }
}